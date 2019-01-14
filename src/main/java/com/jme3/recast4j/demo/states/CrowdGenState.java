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
import com.simsilica.lemur.Label;

/**
 *
 * @author Robert
 */
public class CrowdGenState extends BaseAppState {

    private Container contCrowd;
    
    @Override
    protected void initialize(Application app) {
        
        //Stub container for Crowd configuration.
        contCrowd = new Container(new MigLayout("wrap"));
        contCrowd.setName("TestGenState crowdCont");
        contCrowd.setAlpha(0, false);
        contCrowd.addChild(new Label("Crowd Panel Holder"));
    }

    @Override
    protected void cleanup(Application app) {
        ((SimpleApplication) getApplication()).getGuiNode().detachChild(contCrowd);
    }

    //onEnable()/onDisable() can be used for managing things that should 
    //only exist while the state is enabled. Prime examples would be scene 
    //graph attachment or input listener attachment.
    @Override
    protected void onEnable() {
        getStateManager().attach(new AgentCrowdState());
    }

    @Override
    protected void onDisable() {
        getStateManager().detach(this);
    }
    
    @Override
    public void update(float tpf) {
        //TODO: implement behavior during runtime
    }

    /**
     * @return the contCrowd
     */
    public Container getContCrowd() {
        return contCrowd;
    }
    
}
