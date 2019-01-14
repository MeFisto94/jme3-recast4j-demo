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
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.ColorRGBA;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.RollupPanel;
import com.simsilica.lemur.TabbedPanel;
import com.simsilica.lemur.component.TbtQuadBackgroundComponent;
import com.simsilica.lemur.event.ConsumingMouseListener;
import com.simsilica.lemur.event.CursorEventControl;
import com.simsilica.lemur.event.DragHandler;
import com.simsilica.lemur.event.MouseEventControl;
import com.simsilica.lemur.style.Attributes;
import com.simsilica.lemur.style.BaseStyles;
import com.simsilica.lemur.style.Styles;

/**
 *
 * @author Robert
 */
public class AgentCrowdState extends BaseAppState {

    private Container contTabs;
    
    @Override
    protected void initialize(Application app) {
//        getStateManager().attach(new AgentGenState());
//        getStateManager().attach(new CrowdGenState());
        
        GuiGlobals.initialize(app);
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");
        
        //Make container panels solid.
        Styles styles = GuiGlobals.getInstance().getStyles();
        Attributes attrs = styles.getSelector(Container.ELEMENT_ID, "glass");
        TbtQuadBackgroundComponent bg = attrs.get("background");
        bg.setColor(new ColorRGBA(0.25f, 0.5f, 0.5f, 1.0f));
        
        //Default is pink with alphs .85.
        attrs = styles.getSelector("title", "glass");
        ColorRGBA highlightColor = attrs.get("highlightColor");
        highlightColor.set(new ColorRGBA(ColorRGBA.Pink));
    }

    @Override
    protected void cleanup(Application app) {
        ((SimpleApplication) getApplication()).getGuiNode().detachChild(contTabs);
    }

    @Override
    protected void onEnable() {
        //Create the container that will hold the tab panel for AgentGen and 
        //CrowdGen gui.
        contTabs = new Container();
        contTabs.setName("TestGenState tabsCont");
        //Make it dragable.
        DragHandler dragHandler = new DragHandler();
        CursorEventControl.addListenersToSpatial(contTabs, dragHandler);
        //Consume the mouse events so it doesn't auto-close when clicked
        // outside of interactive child elements.
        MouseEventControl.addListenersToSpatial(contTabs, ConsumingMouseListener.INSTANCE);
        contTabs.addChild(new Label("Detour Crowd"));
        
        //Create the tabbed panel.
        TabbedPanel tabPanel = contTabs.addChild(new TabbedPanel());
        tabPanel.setInsets(new Insets3f(5, 5, 5, 5));
        
        //Add a rollup panel so can hide test genrator.
        RollupPanel rollAgentGen = new RollupPanel("Expand / Collapse", getState(AgentGenState.class).getContAgentGen(), "glass");
        rollAgentGen.setOpen(false);
        rollAgentGen.getTitleElement().setTextHAlignment(HAlignment.Center);
        rollAgentGen.getTitleElement().setFocusColor(ColorRGBA.Magenta);

        //Add a rollup panel so can hide crowd settings.
        RollupPanel rollCrowd = new RollupPanel("Expand / Collapse", getState(CrowdGenState.class).getContCrowd(), "glass");
        rollCrowd.setOpen(false);
        rollCrowd.getTitleElement().setTextHAlignment(HAlignment.Center);
        rollCrowd.getTitleElement().setFocusColor(ColorRGBA.Magenta);
        
        //The tabs to be added. Set alpha here to hide background tab panel.
        tabPanel.addTab("Agent Generator", rollAgentGen);
        tabPanel.addTab("Crowd Settings", rollCrowd);
        
        getState(GuiUtilState.class).centerComp(contTabs);
        
        ((SimpleApplication) getApplication()).getGuiNode().attachChild(contTabs);
    }

    @Override
    protected void onDisable() {
        getStateManager().detach(this);
    }
    
    @Override
    public void update(float tpf) {
        //TODO: implement behavior during runtime
    }
    
}
