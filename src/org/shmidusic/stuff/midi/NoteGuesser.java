package org.shmidusic.stuff.midi;

// this class transforms midi event list to sheet music
// obvious from name, it guesses note lengths from event timestamp and unitsPerSecond

import org.apache.commons.math3.fraction.Fraction;
import org.shmidusic.Main;
import org.shmidusic.sheet_music.SheetMusic;
import org.shmidusic.sheet_music.staff.Staff;
import org.shmidusic.sheet_music.staff.chord.Chord;
import org.shmidusic.sheet_music.staff.staff_config.StaffConfig;
import org.shmidusic.stuff.midi.standard_midi_file.SMF;
import org.shmidusic.stuff.midi.standard_midi_file.Track;
import org.shmidusic.stuff.midi.standard_midi_file.event.*;
import org.shmidusic.stuff.tools.INote;
import org.shmidusic.stuff.tools.Logger;

import javax.swing.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** this class generates shmidusic SheetMusic from Standard Midi File data */
public class NoteGuesser
{
    /** conventional unit count in one second.
      * all midi events have conventional units timestamp, cuz 1000 milliseconds does not divide to 3. (why ain't we babylonians?)
      * I prefer to use 960 as unitsPerSecond, cuz it' really close to 1000 ms */
	final int unitsPerSecond;
    int tempo = 120; // we'll get tempo from some events. pray to your god that it will be in the first track exactly at the 0th second

    final SMF midiFile;

	public NoteGuesser(SMF smf) {
		this.midiFile = smf;
        this.unitsPerSecond = smf.getPPQN();
	}

    public SheetMusic generateSheetMusic(Consumer<SheetMusic> rebuild)
    {
        SheetMusic sheetMusic = new SheetMusic();
        Staff staff = sheetMusic.staffList.get(0);

        List<GuessingNote> notes = new ArrayList<>();

        for (Track track: midiFile.getTrackList()) {

            List<GuessingNote> opened = new ArrayList<>();
            int time = 0;
            for (Event event : track.getEvtList()) {

                time += event.getTime();
                int finalTime = time;

                if (event instanceof NoteOn && ((NoteOn)event).getVelocity() > 0) {

                    NoteOn noteOn = (NoteOn)event;
                    opened.add(new GuessingNote(noteOn.getPitch(), noteOn.getMidiChannel(), time));

                } else if (event instanceof NoteOff || (event instanceof NoteOn && ((NoteOn)event).getVelocity() == 0)) {

                    INoteEvent noteOff = (INoteEvent)event;
                    opened.stream().filter(n -> n.tune == noteOff.getPitch() && n.channel == noteOff.getMidiChannel())
                    .findAny().ifPresent(n ->
                    {
                        n.setDuration(finalTime - n.time);
                        notes.add(n);
                        opened.remove(n);
                    });

                } else {
                    // handle staff config event
                    if (time == 0) {
                        consumeConfigEvent(event, staff.getConfig());
                    } else {
                        Logger.warning("Config event not at the start of midi: " + event.getClass().getSimpleName() + " " + event.getTime() + ". Should we split Staff in future on this case?");
                    }
                }
            }
        }

        notes.stream().sorted((n1,n2) -> n1.time - n2.time).forEach(n -> {
            /** @debug */
//            System.out.println(guessPos(n.time) + " " + n.strMe());
            putAt(guessPos(n.time), n, staff);

            /** @debug */
//            rebuild.accept(sheetMusic);
//            JOptionPane.showMessageDialog(Main.window, "zhopa");
        });

        return sheetMusic;
    }

    private void consumeConfigEvent(Event event, StaffConfig config)
    {
        if (event instanceof TempoEvent) {
            tempo = (int)((TempoEvent) event).getTempo();
//            config.setTempo(tempo);
        } else if (event instanceof PChange) {
            PChange instrument = (PChange) event;
            config.channelList.get(instrument.getMidiChannel()).setInstrument(instrument.getValue());
        } else {
            // ...
        }
    }

    private void putAt(Fraction desiredPos, INote note, Staff staff)
    {
        BiFunction<Fraction, Integer, Integer> putRest = (rest, idx) -> {
            while (rest.compareTo(fr(0,1)) > 0) {

                Fraction greatest = greatest(rest);
                rest = rest.subtract(greatest);
                staff.addNewAccord(idx++).setExplicitLength(greatest);
                staff.accordListChanged(idx - 1);
            }
            return idx;
        };

        Optional<Chord> opt = staff.findChord(desiredPos);
        if (opt.isPresent()) {

            Chord chord = opt.get();
            Fraction pauseRest = chord.getFraction().subtract(note.getRealLength());
            chord.addNewNota(note);
            staff.accordListChanged(-100);

            // putting filler in case when chord length became smaller to preserve timing
            putRest.apply(pauseRest, staff.getChordList().indexOf(chord) + 1);

        } else {
            Optional<Chord> lastChord = staff.getChord(-1);
            Fraction lastChordStart = lastChord.flatMap(c -> staff.findChordStart(c)).orElse(fr(0, 1));
            Fraction lastChordEnd = lastChord.map(c -> c.getFraction()).orElse(fr(0, 1)).add(lastChordStart);

            if (desiredPos.compareTo(lastChordEnd) < 0) {

                // putting preceding pauses
                Chord chord = staff.findClosestBefore(desiredPos).get(); // TODO: for some reason, may return exactly requested, (you request 3/2 and get chord on 3/2) it's wrong
                Fraction preRest = desiredPos.subtract(staff.findChordStart(chord).get());
                Fraction postRest = chord.getFraction().subtract(preRest);

                chord.setExplicitLength(greatest(preRest));
                int index = staff.getChordList().indexOf(chord) + 1;
                index = putRest.apply(preRest.subtract(greatest(postRest)), index);
                // putting note
                staff.addNewAccord(index++).setExplicitLength(postRest).addNewNota(note);
                // putting following pauses
                putRest.apply(postRest.subtract(note.getRealLength()), index);

            } else {
                // put enough pauses
                putRest.apply(desiredPos.subtract(lastChordEnd), staff.getChordList().size());
                // append chord
                staff.addNewAccord().addNewNota(note);
            }
        }

        // TODO: it's broken somehow. Bakemonogatari can be opent with Noteworthy, but cant with midiana
    }

    private Fraction guessPos(int unitsArg)
    {
        Cont<Integer> unitsLeft = new Cont<>(unitsArg);
        Cont<Fraction> result = new Cont<>(fr(0, 1));

        int tactNum = unitsLeft.get() /  toUnits(fr(2,1));
        unitsLeft.set(unitsLeft.get() - tactNum * toUnits(fr(2,1)));
        result.set(result.get().add(fr(2, 1).multiply(tactNum)));

        lengths()
        .stream()
        .sorted()
        .collect(Collectors.toCollection(LinkedList::new))
        .descendingIterator().forEachRemaining(length ->
        {
            if (toUnits(length) <= unitsLeft.get()) {
                result.set(result.get().add(length));
                unitsLeft.set(unitsLeft.get() - toUnits(length));
            }
        });

        return result.get();
    }

//    private Fraction guessPos(int units)
//    {
//        Fraction result = fr(0, 1);
//
//        int tactNum = units /  toUnits(fr(2,1));
//        result = result.add(fr(2, 1).multiply(tactNum));
//
//        // TODO: it's fishy, we don't guarantee, that we add greatest firstly. we may end up
//        // with 3 x 3/16 with leak where it actually was 3 x 1/6. Fix ASAP! I don't wanna rely on correctness of midi event times
//        while (units > toUnits(shortest().subtract(result))) {
//            Fraction largestFit = guessLength(units - toUnits(result));
//            result = result.add(largestFit);
//        }
//
//        return result;
//    }


    private Fraction guessLength(int units)
    {
        List<Fraction> lengths = lengths();

        // plus handle possibility that it is two linked Note-s... or may be even no...
        int error = lengths.stream().map(f -> Math.abs(units - toUnits(f))).sorted().findFirst().get();
        return lengths.stream().filter(f -> Math.abs(units - toUnits(f)) == error).findAny().get();
    }

    private Fraction greatest(Fraction length) {
        Iterable<Fraction> iterable = () -> lengths().descendingIterator();
        return StreamSupport.stream(iterable.spliterator(), false)
                .filter(f -> f.compareTo(length) <= 0)
                .findFirst().get();
    }

    private static LinkedList<Fraction> lengths()
    {
        return Stream.of(
            fr(2, 1),
            // all accepted variations of semibreve: clean | triplet| with dot | with two dots
            fr(1, 1), fr(1, 3), fr(3, 2), fr(7, 4),
            // half
            fr(1, 2), fr(1, 6), fr(3, 4), fr(7, 8),
            // quarter
            fr(1, 2), fr(1, 12), fr(3, 8), fr(7, 16),
            // 1/8 does not have triplet and two dots
            fr(1, 8), fr(3, 16), // TODO: apparently needs triplet...
            // 1/16 does not need triplet and dots
            fr(1,16))
         .sorted(Fraction::compareTo).collect(Collectors.toCollection(LinkedList::new));
    }

    public static <T> LinkedList<T> asList(T... a) {
        LinkedList<T> list = new LinkedList<>();
        for (T elem: a) {
            list.add(elem);
        }
        return list;
    }

    private int toUnits(Fraction length) {
        return length.multiply(unitsPerSecond).intValue();
    }

    private static Fraction fr(int num, int den) {
        return new Fraction(num, den);
    }

    private class GuessingNote implements INote
    {
        final int time;
        final int tune;
        final int channel;
        int duration = -100;

        public GuessingNote(int tune, int channel, int time) {
            this.time = time;
            this.tune = tune;
            this.channel = channel;
        }

        public GuessingNote setDuration(int value) {
            this.duration = value;
            return this;
        }

        public Integer getTune() { return tune; }
        public Integer getChannel() { return channel; }
        public Fraction getLength() {
            return isTriplet()
                    ? getActualLength().multiply(3)
                    : getActualLength();
        }
        public Boolean isTriplet() {
            return getActualLength().getDenominator() % 3 == 0;
        }

        private Fraction getActualLength() {
            return guessLength(this.duration);
        }

        public String strMe() {
            return "time: " + time + "; tune: " + tune + "; channel: " + channel + "; duration: " + duration + "; length: " + getActualLength();
        }
    }

    private class Cont<T>
    {
        private T value;

        public Cont(T value) {
            this.value = value;
        }

        public Cont<T> set(T value) {
            this.value = value;
            return this;
        }

        public T get() {
            return value;
        }
    }
}
