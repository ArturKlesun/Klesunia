package GraphTmp;

import java.awt.Label;

import Musica.Nota;
import Musica.NotnyStan;

public class Status extends Label{
	NotnyStan stan;
	public Status(NotnyStan stan){
		super();
		this.stan = stan;
	}

	public void renew(){
		String s = "�����: " + stan.mode + "  ������ �������: " + stan.stepInOneSys + "  ����� ���: " + stan.noshuCount + ", �����: " + Nota.time;
		setText(s);	
	}
}
