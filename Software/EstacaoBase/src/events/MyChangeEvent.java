/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package events;

import java.util.EventObject;

public class MyChangeEvent extends EventObject {
    // This event definition is stateless but you could always
    // add other information here.
    //http://stackoverflow.com/questions/1658702/how-do-i-make-a-class-extend-observable-when-it-has-extended-another-class-too

    public MyChangeEvent(Object source) {
        super(source);
    }
}