package Gui;

import Gui.staff.Staff;
import Musica.*;
import Gui.staff.pointerable.Accord;
import Gui.staff.pointerable.Nota;
import Gui.staff.pointerable.Phantom;
import Gui.staff.Pointer;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;


final public class SheetMusic extends JPanel {
	JScrollPane scroll;
	
	final public static int NORMAL_HEIGHT = 40;
	final public static int NORMAL_WIDTH = 25;

	double MARGIN_V = 15; // Сколько отступов сделать сверху перед рисованием полосочек // TODO: move it into Constants class maybe? // eliminate it nahuj maybe?
	double MARGIN_H = 1; // TODO: move it into Constants class maybe?
	int maxy = 0;
	int SISDISPLACE = 40;
	
	public static BufferedImage[] vseKartinki = new BufferedImage[7]; // TODO: поменять этот уродский массив на ключ-значение // Гузно у тебя уродское
	public static BufferedImage[] vseKartinki0 = new BufferedImage[7];

	int taktCount = 1;
	int curAccord = -2;
	
	boolean trolling = false;
	
	Staff stan;
	ArrayList<Staff> staffList = new ArrayList();
	public Window parentWindow = null;
		
	public SheetMusic(Window parent) {
		this.parentWindow = parent;

	    System.out.println(getClass().getResource("../").getPath());
	    try {	vseKartinki[0] = ImageIO.read(new File("imgs/vio_sized.png"));
				vseKartinki[1] = ImageIO.read(new File("imgs/bass_sized.png"));
				vseKartinki[2] = ImageIO.read(new File("imgs/flat_sized.png"));
				vseKartinki[6] = ImageIO.read(new File("imgs/sharp_sized.png")); // -_-
				vseKartinki[3] = ImageIO.read(new File("imgs/MyPointer.png"));
				vseKartinki[4] = ImageIO.read(new File("imgs/volume.png"));
				vseKartinki[5] = ImageIO.read(new File("imgs/instrument.png"));
	    } catch (IOException e) { e.printStackTrace(); System.out.println("Темнишь что-то со своей картинкой..."); }
	    for (int i = 0; i < vseKartinki.length; ++i ) {
	        vseKartinki0[i] = vseKartinki[i];
	        vseKartinki[i] = changeSize(i);
	    }

		this.addNewStaff();
		this.stan = this.getFocusedStaff();
	}
	
	public void paintComponent(Graphics g) {

		for (Staff stave: this.getStaffList()) {
			int highestLineY = this.getMarginY();

			g.setColor(Color.WHITE);
			g.fillRect(0,0,this.getWidth(),this.getHeight());
			g.setColor(Color.BLUE);
			g.drawImage(vseKartinki[1], this.getStepWidth(), 11* this.getStepHeight() + highestLineY +2, this);   //bass
			g.drawImage(vseKartinki[0], this.getStepWidth(), highestLineY - 3* this.getStepHeight(), this);       //violin
			int gPos = this.getMarginX() + 4 * this.getStepWidth() -2 * this.getStepWidth();

			taktCount = 1;
			int maxSys = 0;

			int curCislic = 0;
			int curZnamen = 1;
			int lastMaxSys = 1;

			drawPhantom(stave.getPhantom(), g, gPos, highestLineY);
			gPos += 2* this.getStepWidth() * stave.getPhantom().getTakenStepCount();
			for (Accord accord: stave.getAccordList()) {
				// Рисуем нотный стан
				if (gPos >= getStepInOneSysCount() * this.getStepWidth()) {
					// Переходим на новую октаву
					++maxSys;
					g.drawImage(vseKartinki[1], this.getStepWidth(), (SISDISPLACE+11)* this.getStepHeight() + highestLineY +2, this);
					g.drawImage(vseKartinki[0], this.getStepWidth(), highestLineY + (SISDISPLACE-3)* this.getStepHeight(), this);
					for (int j=0; j<(stave.bassKey? 11: 5); ++j){
						if (j == 5) continue;
						g.drawLine(this.getMarginX(), highestLineY + j* this.getStepHeight() *2, this.getWidth() - this.getMarginX()*2, highestLineY + j* this.getStepHeight() *2);
					}


					gPos = this.getMarginX() + 4 * this.getStepWidth();
					highestLineY += SISDISPLACE* this.getStepHeight();

				}

				// TODO: move into staff.getImage()
				if (getFocusedStaff().getFocusedAccord() == accord) { g.drawImage(this.getPointerImage(), gPos + getStepWidth(), highestLineY- this.getStepHeight() *14, this ); }   // Картинка указателя
				

				if (accord.getNotaList().size() > 0) {

					curCislic += accord.getShortest().getNumerator();
					curCislic = drawTaktLineIfNeeded(curCislic, curZnamen, g, gPos, highestLineY);
					g.drawImage(accord.getImage(), gPos, highestLineY - 12 * getStepHeight(), this);
					gPos += 2 * this.getStepWidth() * accord.getTakenStepCount();
				}

				if (maxSys != lastMaxSys) {
					this.setPreferredSize(new Dimension(this.getWidth() - 20, maxy+SISDISPLACE*this.getStepHeight()));	//	Needed for the scroll bars to appear
					lastMaxSys = maxSys;
				}
			}
			
			g.setColor(Color.blue);
			for (int i=0; i<(stave.bassKey? 11: 5); ++i){
				if (i == 5) continue;
				g.drawLine(this.getMarginX(), highestLineY + i* this.getStepHeight() *2, this.getWidth() - this.getMarginX()*2, highestLineY + i* this.getStepHeight() *2);
			}

			if (highestLineY>maxy) maxy=highestLineY;
			highestLineY = (int)Math.round(MARGIN_V* this.getStepHeight());
		}
		
		this.revalidate();	//	Needed to recalc the scroll bars
	}
	
	public void changeScale(int n) {
		Settings.inst().changeScale(n);
		refresh();
	}
	
	public void refresh() {
	    for (int i = 0; i < vseKartinki.length; ++i ) { // >_<
	        vseKartinki[i] = changeSize(i);
	    }
	
	    Nota.refreshSizes(this);
	    maxy = 0;
		Nota.bufInit(this);
	    repaint();
	}
	
	private BufferedImage changeSize(int idx) { // TODO
	    int w0 = vseKartinki0[idx].getWidth();
	    int h0 = vseKartinki0[idx].getHeight();
	    int w1 = w0*this.getNotaWidth()/NORMAL_WIDTH;
	    int h1 = h0*this.getNotaHeight()/NORMAL_HEIGHT;
	
	    BufferedImage tmp = new BufferedImage(w1, h1, BufferedImage.TYPE_INT_ARGB);
	    Graphics g = tmp.createGraphics();
	    Image scaledImage = vseKartinki0[idx].getScaledInstance(w1, h1, Image.SCALE_SMOOTH);
	    g.drawImage(scaledImage, 0, 0, w1, h1, null);
	    g.dispose();
	    return tmp;
	}
	
	int tp = 0;
	
	private void drawTriolLine(int x0, int y0, int x1, int thisY, Graphics g ) {
//		x1 += (15)*this.getNotaHeight()/NORMAL_HEIGHT;
//		y1 = thisY;
//		--x0; --x1;
//		g.setColor(Color.BLACK);
//		if (x1 > x0) {
//			g.drawLine(x0,y0, x1,y1);
//			g.drawLine(x0,y0+1, x1,y1+1);
//			g.drawLine(x0,y0+2, x1,y1+2);
//			g.drawString("3",x0+(x1-x0)/2 - this.getNotaWidth()/2, y0+(y1-y0)/2 - this.getNotaHeight()/4);
//		} else {
//			int length = this.getStepWidth()*3;
//			g.drawLine(x0,y0, x0 + length,y0);
//			g.drawLine(x0,y0+1, x0 + length,y0+1);
//			g.drawLine(x0,y0+2, x0 + length,y0+2);
//			g.drawString("3",x0+this.getStepWidth() - this.getNotaWidth()/2, y0 - this.getNotaHeight()/4);
//			g.drawLine(x1,y1, x1 - length,y1);
//			g.drawLine(x1,y1+1, x1 - length,y1+1);
//			g.drawLine(x1,y1+2, x1 - length,y1+2);
//			g.drawString("3",x1-this.getStepWidth() - this.getNotaWidth()/2, y1 - this.getNotaHeight()/4);
//		}
	}
	
	private void drawPhantom(Phantom phantomka, Graphics g, int xIndent, int yIndent) {
		int dX = this.getNotaWidth()/5, dY = this.getNotaHeight()*2;
		g.drawImage(phantomka.getImage(), xIndent - dX, yIndent - dY, this);
		int deltaY = 0, deltaX = 0;
		switch (phantomka.changeMe) {
			case cislicelj:	deltaY += 9 * this.getStepHeight(); break;
			case znamenatelj: deltaY += 11 * this.getStepHeight(); break;
			case tempo: deltaY -= 1 * this.getStepHeight(); break;
			case instrument: deltaY += 4 * this.getStepHeight(); deltaX += this.getStepWidth() / 4; break;
			case volume: deltaY += 24 * this.getStepHeight(); break;
			default: out("Неизвестный енум в ДоуПанеле"); break;
		}
		g.drawImage(this.getPointerImage(), xIndent - 7*this.getNotaWidth()/25 + deltaX, yIndent - this.getStepHeight() * 14 + deltaY, this);
	}
	
	public void checkCam(){
	    JScrollBar vertical = scroll.getVerticalScrollBar();
	    vertical.setValue( SISDISPLACE* this.getStepHeight() * (Pointer.pos / (getStepInOneSysCount() / 2 - 2) -1) );
	    repaint();
	}
	
	public void page(int pageCount) {
		JScrollBar vertical = scroll.getVerticalScrollBar();
		int pos = vertical.getValue()+pageCount*SISDISPLACE* this.getStepHeight();
		if (pos<0) pos = 0;
		if (pos>vertical.getMaximum()) pos = vertical.getMaximum();
		vertical.setValue(pos);
		repaint();
	}
	
	private int drawTaktLineIfNeeded(int curCislic, int curZnamen, Graphics g, int xIndent, int yIndent) {
		if (curCislic >= stan.cislic) {
	    	curCislic %= stan.cislic;
	        g.setColor(Color.BLACK);
	        if (curCislic > 0) {
	            g.setColor(Color.BLUE);
	        }
	        g.drawLine(xIndent + this.getStepWidth() * 2, yIndent - this.getStepHeight() * 5, xIndent + this.getStepWidth() * 2, yIndent + this.getStepHeight() * 20);
	        g.setColor(Color.decode("0x00A13E"));
	        g.drawString(taktCount + "", xIndent + this.getStepWidth(), yIndent - this.getStepHeight() * 9);
	
	        ++taktCount;
		}
		return curCislic;
	}
	
	int getSys(int n){
		return n/(getStepInOneSysCount() / 2 - 2);
	}
	
	private void out(String str) {
		System.out.println(str);
	}

	// getters/setters

	public ArrayList<Staff> getStaffList() {
		ArrayList staveList = (ArrayList)this.staffList.clone();
		staveList.add(this.stan);
		return staveList;
	}

	public Staff getFocusedStaff() {
		// TODO: do something
		return this.getStaffList().get(0);
	}

	public SheetMusic addNewStaff() {
		this.staffList.add(new Staff(this));
		return this;
	}

	public BufferedImage getPointerImage() {
		return this.vseKartinki[3];
	}

	public BufferedImage getFlatImage() {
		return this.vseKartinki[2];
	}

	public BufferedImage getSharpImage() {
		return this.vseKartinki[6];
	}

	public int getStepInOneSysCount() {
		return (int)Math.floor(this.getWidth() / this.getStepWidth() - 2 * this.MARGIN_H);
	}

	// TODO: use from Settings

	public int getNotaWidth() {
		return Settings.inst().getNotaWidth();
	}

	public int getNotaHeight() {
		return Settings.inst().getNotaHeight();
	}

	public int getStepWidth() {
		return Settings.inst().getStepWidth();
	}

	public int getStepHeight() {
		return Settings.inst().getStepHeight();
	}

	// Until here

	public int getMarginX() {
		return (int)Math.round(MARGIN_H * this.getStepWidth());
	}

	public int getMarginY() {
		return (int)Math.round(MARGIN_V * this.getStepHeight());
	}
}

