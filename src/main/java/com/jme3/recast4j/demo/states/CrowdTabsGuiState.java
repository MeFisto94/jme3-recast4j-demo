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
import com.jme3.recast4j.demo.layout.MigLayout;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.RollupPanel;
import com.simsilica.lemur.TabbedPanel;
import com.simsilica.lemur.event.CursorEventControl;
import com.simsilica.lemur.event.DragHandler;

/**
 *
 * @author Robert
 */
public class CrowdTabsGuiState extends BaseAppState {

    private Container contAgentSettingsGui;
    
    @Override
    protected void initialize(Application app) {
    }

    @Override
    protected void cleanup(Application app) {
        //Removing will also cleanup the AgentGui and CrowdGui.
        ((SimpleApplication) getApplication()).getGuiNode().detachChild(contAgentSettingsGui);
    }

    @Override
    protected void onEnable() {
        //Create the container that will hold the tab panel for AgentGridGui and 
        //AgentSettings gui.
        contAgentSettingsGui = new Container(new MigLayout("wrap"));
        contAgentSettingsGui.setName("AgentSettingsGui contAgentSettingsGui");
        //Make it dragable.
        DragHandler dragHandler = new DragHandler();
        CursorEventControl.addListenersToSpatial(contAgentSettingsGui, dragHandler);
        contAgentSettingsGui.addChild(new Label("Detour Crowd"));
        
        //Create the tabbed panel.
        TabbedPanel tabPanel = contAgentSettingsGui.addChild(new TabbedPanel());
        
        //Add a rollup panel so can hide agent grid generator.
        RollupPanel rollAgentGrid = new RollupPanel("Expand / Collapse", 
                getState(AgentGridGuiState.class).getContAgentGridGui(), "glass");
        rollAgentGrid.getTitleElement().setTextHAlignment(HAlignment.Center);
        rollAgentGrid.setAlpha(0, false);
        tabPanel.addTab("Agent Grid Generator", rollAgentGrid);
        
        //Add a rollup panel so can hide agent settings.
        RollupPanel rollAgentSettings = new RollupPanel("Expand / Collapse", 
                getState(AgentSettingsGuiState.class).getContAgentSettingsGui(), "glass");
        rollAgentSettings.getTitleElement().setTextHAlignment(HAlignment.Center);
        rollAgentSettings.setAlpha(0, false);        
        tabPanel.addTab("Agent Settings", rollAgentSettings);
        
        getState(GuiUtilState.class).centerComp(contAgentSettingsGui);
        
        ((SimpleApplication) getApplication()).getGuiNode().attachChild(contAgentSettingsGui);
    }

    @Override
    protected void onDisable() {
        //Called by the DemoApplication F1 button ActionListener.
        getStateManager().detach(this);
    }
    
    @Override
    public void update(float tpf) {
        //TODO: implement behavior during runtime
    }
    
}
