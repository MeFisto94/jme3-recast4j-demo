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
import com.jme3.input.KeyInput;
import com.jme3.math.ColorRGBA;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.component.TbtQuadBackgroundComponent;
import static com.simsilica.lemur.focus.FocusNavigationFunctions.F_X_AXIS;
import static com.simsilica.lemur.focus.FocusNavigationFunctions.F_Y_AXIS;
import com.simsilica.lemur.style.Attributes;
import com.simsilica.lemur.style.BaseStyles;
import com.simsilica.lemur.style.Styles;

/**
 *
 * @author Robert
 */
public class LemurConfigState extends BaseAppState {
    
    @Override
    protected void initialize(Application app) {
        GuiGlobals.initialize(app);
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");
        
        //Block Lemur from mapping input.
        GuiGlobals.getInstance().getInputMapper().map(F_X_AXIS, KeyInput.KEY_RIGHT );
        GuiGlobals.getInstance().getInputMapper().map(F_Y_AXIS, KeyInput.KEY_RIGHT );
        
        //Make container panels solid.
        Styles styles = GuiGlobals.getInstance().getStyles();
        Attributes attrs = styles.getSelector(Container.ELEMENT_ID, "glass");
        TbtQuadBackgroundComponent bg = attrs.get("background");
        bg.setColor(new ColorRGBA(0.25f, 0.5f, 0.5f, 1.0f));
        
        //Set the rollup button colors
        //Default is pink with alpha .85. 
        attrs = styles.getSelector("title", "glass");
        attrs.set("highlightColor", new ColorRGBA(ColorRGBA.Pink));
        attrs.set("focusColor", new ColorRGBA(ColorRGBA.Magenta));
        
        //Set the default font size
        attrs = styles.getSelector("glass");
        attrs.set("fontSize", 13);

        //Change textfield background from defaults.
        attrs = styles.getSelector(TextField.ELEMENT_ID, "glass");
        attrs.set("background", new QuadBackgroundComponent(new ColorRGBA(ColorRGBA.DarkGray)), false);
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
        
    }

    @Override
    protected void onDisable() {
        //Called when the state was previously enabled but is now disabled 
        //either because setEnabled(false) was called or the state is being 
        //cleaned up.
    }
    
    @Override
    public void update(float tpf) {
        
    }
    
}
