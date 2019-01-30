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

package com.jme3.recast4j.demo.controls;

import com.jme3.math.ColorRGBA;
import com.jme3.recast4j.Detour.Crowd.Crowd;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.control.AbstractControl;
import org.recast4j.detour.crowd.CrowdAgent;

/**
 *
 * @author Robert
 */
public class DebugMoveControl extends AbstractControl {
    
    private CrowdAgent agent;
    private Crowd crowd;
    private Geometry halo;
    private final ColorRGBA white;
    private final ColorRGBA cyan;
    private final ColorRGBA magenta;
    private final ColorRGBA black;  
    private float timer;
    
    public DebugMoveControl(Crowd crowd, CrowdAgent agent, Geometry halo) {
        this.crowd = crowd;
        this.agent = agent;
        this.halo = halo;
        this.white = new ColorRGBA(ColorRGBA.White);
        this.cyan = new ColorRGBA(ColorRGBA.Cyan);
        this.magenta = new ColorRGBA(ColorRGBA.Magenta);
        this.black = new ColorRGBA(ColorRGBA.Black);
    }

    @Override
    protected void controlUpdate(float tpf) {
        timer++;
        //one time per second or so.
        if (timer > 1000) {
            if (crowd.isForming(agent)) {
                halo.getMaterial().setColor("Color", white);
            } else if (crowd.isMoving(agent)) {
                halo.getMaterial().setColor("Color", magenta);
            } else if (crowd.hasNoTarget(agent)) {
                halo.getMaterial().setColor("Color", cyan);
            } else {
                halo.getMaterial().setColor("Color", black);
            }
            timer = 0;
        }
    }

    /**
     * @param agent the agent to set
     */
    public void setAgent(CrowdAgent agent) {
        this.agent = agent;
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }

    /**
     * @param halo the halo to set
     */
    public void setHalo(Geometry halo) {
        this.halo = halo;
    }

    /**
     * @param crowd the crowd to set
     */
    public void setCrowd(Crowd crowd) {
        this.crowd = crowd;
    }

}

