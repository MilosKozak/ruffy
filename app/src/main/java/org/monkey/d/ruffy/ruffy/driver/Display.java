package org.monkey.d.ruffy.ruffy.driver;

public class rtFrame {
		public int index;
		public String[] row1 = new String[8];
		public String[] row2 = new String[8];
		public String[] row3 = new String[8];
		public String[] row4 = new String[8];
		public boolean r1,r2,r3,r4;
		public byte reason;
		
		public rtFrame()
		{
			index = -1;						//This is an invalid value (valid are 0-255)
			r1 = r2 = r3 = r4 = false;		//Invalidate rows
		}
		
		public void addR1(String[] s)
		{
			row1 = s;
			r1 = true;
		}
		
		public void addR2(String[] s)
		{
			row2 = s;
			r2 = true;
		}
		
		public void addR3(String[] s)
		{
			row3 = s;
			r3 = true;
		}
		public void addR4(String[] s)
		{
			row4 = s;
			r4 = true;
		}
		
		public boolean isComplete()
		{
			if(r1 && r2 && r3 &&r4)
				return true;
			else
				return false;
		}
	}