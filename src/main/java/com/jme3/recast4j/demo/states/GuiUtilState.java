/*
 * The MIT License
 *
 * Copyright 2019 .
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * MODELS/DUNE.J3O:
 * Converted from http://quadropolis.us/node/2584 [Public Domain according to the Tags of this Map]
 */

package com.jme3.recast4j.demo.states;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.recast4j.demo.layout.MigLayout;
import com.simsilica.lemur.ActionButton;
import com.simsilica.lemur.CallMethodAction;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.Panel;
import java.text.NumberFormat;
import java.text.ParsePosition;

/**
 * Utility class for common use Gui methods.
 * 
 * @author Robert
 */
public class GuiUtilState extends BaseAppState {
    
    @Override
    protected void initialize(Application app) {
        //It is technically safe to do all initialization and cleanup in the 
        //onEnable()/onDisable() methods. Choosing to use initialize() and 
        //cleanup() for this is a matter of performance specifics for the 
        //implementor.
        //TODO: initialize your AppState, e.g. attach spatials to rootNode
    }

    @Override
    protected void cleanup(Application app) {
        //TODO: clean up what you initialized in the initialize method,
        //e.g. remove all spatials from rootNode
    }

    //onEnable()/onDisable() can be used for managing things that should 
    //only exist while the state is enabled. Prime examples would be scene 
    //graph attachment or input listener attachment.
    @Override
    protected void onEnable() {
        //Called when the state is fully enabled, ie: is attached and 
        //isEnabled() is true or when the setEnabled() status changes after the 
        //state is attached.
    }

    @Override
    protected void onDisable() {
        //Called when the state was previously enabled but is now disabled 
        //either because setEnabled(false) was called or the state is being 
        //cleaned up.
    }
    
    @Override
    public void update(float tpf) {
        //TODO: implement behavior during runtime
    }

    /**
     * Builds a popup for display of messages.
     * 
     * @param msg the message for the pop to display.
     * @param maxWidth max width for the displayed popup. Used for auto wrap of 
     * text. A size of 0 means use no max width setting.
     * @return the popup to display. 
     */
    public Panel buildPopup( String msg, float maxWidth ) {
        Container window = new Container(new MigLayout("wrap"));
        Label label = window.addChild(new Label(msg));
        if (maxWidth > 0) {
            label.setMaxWidth(maxWidth);
        }
        window.addChild(new ActionButton(new CallMethodAction("Close", window, "removeFromParent")), "align 50%");
        centerComp(window);
        return window;                                                              
    }
    
    /**
     * Centers any lemur component to the middle of the screen.
     * 
     * @param panel the lemur component to center.
     */
    public void centerComp(Panel panel) {
        // Position the panel                                                            
        panel.setLocalTranslation((getApplication().getCamera().getWidth() - panel.getPreferredSize().x)/2, 
                (getApplication().getCamera().getHeight() + panel.getPreferredSize().y)/2, 0);
    }
    
    //Validate user input for float fields.
    public boolean isNumeric(String str) {
        NumberFormat formatter = NumberFormat.getInstance();
        ParsePosition pos = new ParsePosition(0);
        formatter.parse(str, pos);
        return str.length() == pos.getIndex();
    }
}
