/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/ 
 */
package org.openl.tablets.tutorial1;

import java.util.Calendar;

import org.openl.rules.runtime.RuleEngineFactory;

/**
 * @author snshor
 *
 * Run this class "as Java Application". As you progress with tutorial 
 * uncomment appropriate code.
 * 
 *
 */



public class Tutorial1Main {

	//Creates new instance of Java Wrapper for our lesson
	static Tutorial_1RulesInterface tut1;
	
	static {
	    tut1 = new RuleEngineFactory<Tutorial_1RulesInterface>(Tutorial_1RulesInterface.__src, 
	            Tutorial_1RulesInterface.class).makeInstance();
	}

	public static void main(String[] args) 
	{
		
		//Get current hour
		Calendar calendar = Calendar.getInstance();
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		
		
		//Step 1
		// Call the method wrapping Decision Table "hello1"
		tut1.hello1(hour);
		


		// Step 2
		String greeting = null;
		//greeting = tut1.hello2(hour);
		//greeting = tut1.hello21(hour);
		//greeting = tut1.hello22(hour);
		
		
		//Step 3
		//greeting = tut1.hello3(hour);
		if (greeting != null)
			System.out.println(greeting);
		
		
		
		
	}
}
