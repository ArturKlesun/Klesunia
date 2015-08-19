package org.sheet_midusic.staff;

import org.sheet_midusic.staff.chord.Chord;
import org.sheet_midusic.staff.chord.ChordComponent;
import org.sheet_midusic.staff.chord.nota.Nota;
import org.klesun_model.AbstractPainter;
import org.apache.commons.math3.fraction.Fraction;
import org.sheet_midusic.staff.chord.nota.NoteComponent;
import org.sheet_midusic.staff.staff_config.KeySignature;
import org.sheet_midusic.staff.staff_config.StaffConfigComponent;
import org.sheet_midusic.staff.staff_panel.StaffComponent;
import org.sheet_midusic.stuff.graphics.ImageStorage;
import org.sheet_midusic.stuff.tools.jmusic_integration.INota;

import java.awt.*;

public class StaffPainter extends AbstractPainter
{
	public StaffPainter(StaffComponent context, Graphics2D g, int x, int y) {
		// this trick won't do when Painter becames child of Staff
		super(context, g, x + context.staff.getMarginX() + 3 * dx(), y + context.staff.getMarginY()); // 3dx - violin/bass keys and Config
	}

	public void draw(Boolean completeRepaint)
	{
		StaffComponent comp = (StaffComponent)context;
		Staff s = comp.staff;

		Staff.TactMeasurer tactMeasurer = new Staff.TactMeasurer(s.getConfig().getTactSize());

		int i = 0;
		for (java.util.List<Chord> row : s.getAccordRowList(comp.getWidth())) {

			int y = i * Staff.SISDISPLACE * dy(); // bottommest y nota may be drawn on

			drawStaffLines(y);

			int j = 0;
			for (Chord chord : row) {
				int x = j * (2 * dx());

//				ChordComponent chordComp = comp.findChild(chord);
//				KeySignature siga = s.getConfig().getSignature();
//				drawModel((g, xArg, yArg) -> chordComp.drawOn(g, xArg, yArg, siga), x, y - 12 * dy());

				if (tactMeasurer.inject(chord)) { // TODO: broken. we cant draw it separately from chords (when you resize they go assync)
					drawTactLine(x + dx() * 2, y, tactMeasurer);
				}

				++j;
			}

			if (completeRepaint) {
				drawImage(ImageStorage.inst().getViolinKeyImage(), - 3 * dx(), y - 3 * dy());
				drawImage(ImageStorage.inst().getBassKeyImage(), - 3 * dx(), 11 * dy() + y);
			}

			++i;
		}

		StaffConfigComponent configComp = s.getConfig().makeComponent(comp);
		drawModel((g, xArg, yArg) -> configComp.drawOn(g, xArg, yArg), -dx(), 0);
	}

	private void drawTactLine(int x, int baseY, Staff.TactMeasurer tactMeasurer)
	{
		Color lineColor = tactMeasurer.sumFraction.equals(new Fraction(0))
			? Color.BLACK
			: Color.RED;

		drawLine(x, baseY - dy() * 5, x, baseY + dy() * 20, lineColor);
		Color numberColor = new Color(0, 161, 62);
		drawString(tactMeasurer.tactCount + "", x, baseY - dy() * 6, numberColor);
	}

	private void drawStaffLines(int y)
	{
		StaffComponent comp = (StaffComponent)context;
		Staff s = comp.staff;

		int tune = INota.nextIvoryTune(INota.nextIvoryTune(Nota.FA + 12 * 2));

		// normal Nota height lines
		for (int j = 0; j < 11; ++j) {
			tune = INota.prevIvoryTune(INota.prevIvoryTune(tune));
			if (j == 5) continue;
			int lineY = y + j * dy() * 2;
			drawLine(-3 * dx(), lineY, comp.getWidth() - s.getMarginX() * 6, lineY, new Color(128,128,255));

			Rectangle measureFrame = new Rectangle(comp.getWidth() - s.getMarginX() * 6 + dx() / 6, lineY - dy() - 1, dx(), dy() * 2);
			drawString(tune + "", measureFrame, Color.BLUE);
		}

		// hidden Nota height lines fot way too high Nota-s
		for (int j = -3; j >= -5; --j) { // -3 - Mi; -5 -si
			int lineY = y + j * dy() * 2;
			drawLine(- 3 * dx(), lineY, comp.getWidth() - s.getMarginX() * 6, lineY, Color.LIGHT_GRAY);
		}
	}
}