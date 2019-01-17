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
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.ColorRGBA;
import com.jme3.recast4j.demo.layout.MigLayout;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.ActionButton;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.CallMethodAction;
import com.simsilica.lemur.Checkbox;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.ListBox;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.event.DefaultMouseListener;
import com.simsilica.lemur.event.MouseEventControl;
import com.simsilica.lemur.event.PopupState;
import com.simsilica.lemur.text.DocumentModelFilter;
import org.recast4j.detour.crowd.CrowdAgentParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Robert
 */
public class AgentSettingsGuiState extends BaseAppState {

    private static final Logger LOG = LoggerFactory.getLogger(AgentSettingsGuiState.class.getName());
    
    private Container contAgentSettingsGui;
    private DocumentModelFilter doc;
    private TextField fieldColQueryRange;
    private TextField fieldHeight;
    private TextField fieldMaxAccel;
    private TextField fieldMaxSpeed;
    private TextField fieldPathOptimizeRange;
    private TextField fieldRadius;
    private TextField fieldSeparationWeight;
    private ListBox listBoxAvoidance;
    private Checkbox checkAvoid;
    private Checkbox checkSep;
    private Checkbox checkTopo;
    private Checkbox checkTurns;
    private Checkbox checkVis;
    
    @SuppressWarnings("unchecked")
    @Override
    protected void initialize(Application app) {
        
        //The top container for this gui.
        contAgentSettingsGui = new Container(new MigLayout(null));
        contAgentSettingsGui.setName("AgentSettingsGuiState contAgentGridGui");
        contAgentSettingsGui.setAlpha(0, false);
        
        //Holds all the underlying components for this gui.
        Container contMain = new Container(new MigLayout(null));
        contMain.setName("AgentSettingsGuiState contMain");
        contMain.setAlpha(0, false);
        contAgentSettingsGui.addChild(contMain);

        //Begin the crowd agent parameters section.
        contMain.addChild(new Label("Crowd Agent Parameters"), "wrap");
        
        //The agent radius field. 
        contMain.addChild(new Label("Agent Radius"), "split 3, growx");
        doc = new DocumentModelFilter();
        //placeholder for filtering        
        fieldRadius = contMain.addChild(new TextField(doc));
        fieldRadius.setSingleLine(true);
        fieldRadius.setPreferredWidth(50);
        fieldRadius.setText("0.6");
        contMain.addChild(new Label(".float"), "wrap");
        
        //The agent height field. 
        contMain.addChild(new Label("Agent Height"), "split 3, growx");
        doc = new DocumentModelFilter();
        //placeholder for filtering        
        fieldHeight = contMain.addChild(new TextField(doc));
        fieldHeight.setSingleLine(true);
        fieldHeight.setPreferredWidth(50);
        fieldHeight.setText("2.0");
        contMain.addChild(new Label(".float"), "wrap");
        
        //The max acceleration field.
        contMain.addChild(new Label("Max Acceleration"), "split 3, growx");
        doc = new DocumentModelFilter();
        //placeholder for filtering        
        fieldMaxAccel = contMain.addChild(new TextField(doc));
        fieldMaxAccel.setSingleLine(true);
        fieldMaxAccel.setPreferredWidth(50);
        fieldMaxAccel.setText("8.0");
        contMain.addChild(new Label(".float"), "wrap");
        
        //The max speed field.
        contMain.addChild(new Label("Max Speed"), "split 3, growx");
        doc = new DocumentModelFilter();
        //placeholder for filtering        
        fieldMaxSpeed = contMain.addChild(new TextField(doc));
        fieldMaxSpeed.setSingleLine(true);
        fieldMaxSpeed.setPreferredWidth(50);
        fieldMaxSpeed.setText("3.5");
        contMain.addChild(new Label(".float"), "wrap");
        
        //The collision query range.
        contMain.addChild(new Label("Collision Query Range"), "split 3, growx");
        doc = new DocumentModelFilter();
        //placeholder for filtering        
        fieldColQueryRange = contMain.addChild(new TextField(doc));
        fieldColQueryRange.setSingleLine(true);
        fieldColQueryRange.setPreferredWidth(50);
        fieldColQueryRange.setText("12.0");
        contMain.addChild(new Label(".float"), "wrap");
        
        //The path optimization range.
        contMain.addChild(new Label("Path Optimize Range"), "split 3, growx");
        doc = new DocumentModelFilter();
        //placeholder for filtering        
        fieldPathOptimizeRange = contMain.addChild(new TextField(doc));
        fieldPathOptimizeRange.setSingleLine(true);
        fieldPathOptimizeRange.setPreferredWidth(50);
        fieldPathOptimizeRange.setText("30.0");
        contMain.addChild(new Label(".float"), "wrap");
        
        //The separation weight.
        contMain.addChild(new Label("Separation Weight"), "split 3, growx");
        doc = new DocumentModelFilter();
        //placeholder for filtering        
        fieldSeparationWeight = contMain.addChild(new TextField(doc));
        fieldSeparationWeight.setSingleLine(true);
        fieldSeparationWeight.setPreferredWidth(50);
        fieldSeparationWeight.setText("2.0");
        contMain.addChild(new Label(".float"), "wrap");
                
        //The update flags and avoidance labels.
        contMain.addChild(new Label("Avoidance Type"), "split 2");
        contMain.addChild(new Label("Update Flags"), "gapleft 10, wrap");
        
        //Obstacle avoidance listbox.
        listBoxAvoidance = contMain.addChild(new ListBox(), "split 2, gapleft 30");
        listBoxAvoidance.setVisibleItems(7);
        //Have to set this here since Crowd has package-private access the to 
        //the DT_CROWD_MAX_OBSTAVOIDANCE_PARAMS variable. Currently this is eight.
        for (int i = 0; i <= 8; i++) {
            listBoxAvoidance.getModel().add(i);
        }
        listBoxAvoidance.getSelectionModel().setSelection(0);
        
        //Update flags.
        Container contUpdateFlags = new Container(new MigLayout("wrap"));
        contUpdateFlags.setAlpha(0, false);
        contMain.addChild(contUpdateFlags, "wrap, gapleft 30");
        checkTurns = contUpdateFlags.addChild(new Checkbox("ANTICIPATE_TURNS"));        
        checkAvoid = contUpdateFlags.addChild(new Checkbox("OBSTACLE_AVOIDANCE"));
        checkTopo = contUpdateFlags.addChild(new Checkbox("OPTIMIZE_TOPO"));
        checkVis = contUpdateFlags.addChild(new Checkbox("OPTIMIZE_VIS"));
        checkSep = contUpdateFlags.addChild(new Checkbox("SEPARATION"));
        
        //Holds the Legend and Setup buttons.
        Container contButton = new Container(new MigLayout("wrap 2", // Layout Constraints
                "[]push[][]")); // Column constraints [min][pref][max]
        contButton.setName("AgentSettingsGuiState contButton");
        contMain.addChild(contButton, "growx");
        //Buttons.
        contButton.addChild(new ActionButton(new CallMethodAction("Legend", this, "showLegend")));
        Button butSetup = contButton.addChild(new Button("Add Agent Parameters"));
        MouseEventControl.addListenersToSpatial(butSetup, new DefaultMouseListener() {
            //Default minimums.
            private float radius                = 0.0f;
            private float height                = 0.01f;
            private float max_accel             = 0.0f;
            private float max_speed             = 0.0f;   
            private float colQueryRange         = 0.01f;
            private float pathOptimizeRange     = 0.01f;
            private int updateFlags             = 1;
            private int obstacleAvoidanceType   = 0;
            private float separationWeight      = 0.01f;
            
            @Override
            protected void click( MouseButtonEvent event, Spatial target, Spatial capture ) {
                                
                //##### BEGIN CROWD AGENT PARAMS #####
                //Set the CrowdAgentParams.            

                //The agent radius.
                if (!getState(GuiUtilState.class).isNumeric(fieldRadius.getText()) 
                ||  fieldRadius.getText().isEmpty()) {
                    GuiGlobals.getInstance().getPopupState()
                            .showModalPopup(getState(GuiUtilState.class)
                                    .buildPopup("[ Agent Radius ] requires a valid float value.", 0));
                    return;
                } else {
                    radius = new Float(fieldRadius.getText());
                    //Stop negative radius input.
                    if (radius < 0.0f) {
                        GuiGlobals.getInstance().getPopupState()
                                .showModalPopup(getState(GuiUtilState.class)
                                        .buildPopup("[ Agent Radius ] requires a float value >= 0.0f.", 0));
                        return;
                    }
                }
                
                //The agent height.
                if (!getState(GuiUtilState.class).isNumeric(fieldHeight.getText()) 
                ||  fieldHeight.getText().isEmpty()) {
                    GuiGlobals.getInstance().getPopupState()
                            .showModalPopup(getState(GuiUtilState.class)
                                    .buildPopup("[ Agent Height ] requires a valid float value.", 0));
                    return;
                } else {
                    height = new Float(fieldHeight.getText());
                    //Stop negative height input.
                    if (height <= 0.0f) {
                        GuiGlobals.getInstance().getPopupState()
                                .showModalPopup(getState(GuiUtilState.class)
                                        .buildPopup("[ Agent Height ] requires a float value > 0.0f.", 0));
                        return;
                    }
                }
                
                //The max acceleration settings.
                if (!getState(GuiUtilState.class).isNumeric(fieldMaxAccel.getText()) 
                ||  fieldMaxAccel.getText().isEmpty()) {
                    GuiGlobals.getInstance().getPopupState()
                            .showModalPopup(getState(GuiUtilState.class)
                                    .buildPopup("[ Max Acceleration ] requires a valid float value.", 0));
                    return;
                } else {
                    max_accel = new Float(fieldMaxAccel.getText());
                    //Stop negative acceleration input.
                    if (max_accel < 0.0f) {
                        GuiGlobals.getInstance().getPopupState()
                                .showModalPopup(getState(GuiUtilState.class)
                                        .buildPopup("[ Max Acceleration ] requires a float value >= 0.0f.", 0));
                        return;
                    }
                }
                
                //The max speed settings.
                if (!getState(GuiUtilState.class).isNumeric(fieldMaxSpeed.getText()) 
                ||  fieldMaxSpeed.getText().isEmpty()) {
                    GuiGlobals.getInstance().getPopupState()
                            .showModalPopup(getState(GuiUtilState.class)
                                    .buildPopup("[ Max Speed ] requires a valid float value.", 0));
                    return;
                } else {
                    max_speed = new Float(fieldMaxSpeed.getText());
                    //Stop negative speed input.
                    if (max_speed < 0.0f) {
                        GuiGlobals.getInstance().getPopupState()
                                .showModalPopup(getState(GuiUtilState.class)
                                        .buildPopup("[ Max Speed ] requires a float value >= 0.0f.", 0));
                        return;
                    }
                }
                
                //The collision query range.
                if (!getState(GuiUtilState.class).isNumeric(fieldColQueryRange.getText()) 
                ||  fieldColQueryRange.getText().isEmpty()) {
                    GuiGlobals.getInstance().getPopupState()
                            .showModalPopup(getState(GuiUtilState.class)
                                    .buildPopup("[ Collision Query Range ] requires a valid float value.", 0));
                    return;
                } else {
                    colQueryRange = new Float(fieldColQueryRange.getText());
                    //Stop negative height input.
                    if (colQueryRange <= 0.0f) {
                        GuiGlobals.getInstance().getPopupState()
                                .showModalPopup(getState(GuiUtilState.class)
                                        .buildPopup("[ Collision Query Range ] requires a float value > 0.0f.", 0));
                        return;
                    }
                }
                
                //The path optimize range.
                if (!getState(GuiUtilState.class).isNumeric(fieldPathOptimizeRange.getText()) 
                ||  fieldPathOptimizeRange.getText().isEmpty()) {
                    GuiGlobals.getInstance().getPopupState()
                            .showModalPopup(getState(GuiUtilState.class)
                                    .buildPopup("[ Path Optimize Range ] requires a valid float value.", 0));
                    return;
                } else {
                    pathOptimizeRange = new Float(fieldPathOptimizeRange.getText());
                    //Stop negative height input.
                    if (pathOptimizeRange <= 0.0f) {
                        GuiGlobals.getInstance().getPopupState()
                                .showModalPopup(getState(GuiUtilState.class)
                                        .buildPopup("[ Path Optimize Range ] requires a float value > 0.0f.", 0));
                        return;
                    }
                }
                            
                //The separation weight settings.
                if (!getState(GuiUtilState.class).isNumeric(fieldSeparationWeight.getText()) 
                ||  fieldSeparationWeight.getText().isEmpty()) {
                    GuiGlobals.getInstance().getPopupState()
                            .showModalPopup(getState(GuiUtilState.class)
                                    .buildPopup("[ Separation Weight ] requires a valid float value.", 0));
                    return;
                } else {
                    separationWeight = new Float(fieldSeparationWeight.getText());
                    //Stop negative speed input.
                    if (separationWeight < 0.0f) {
                        GuiGlobals.getInstance().getPopupState()
                                .showModalPopup(getState(GuiUtilState.class)
                                        .buildPopup("[ Separation Weight ] requires a float value >= 0.0f.", 0));
                        return;
                    }
                }
                
                //The update flags settings.
                if (!checkTurns.isChecked() 
                &&  !checkAvoid.isChecked() 
                &&  !checkTopo.isChecked() 
                &&  !checkVis.isChecked() 
                &&  !checkSep.isChecked()) {
                    GuiGlobals.getInstance().getPopupState()
                            .showModalPopup(getState(GuiUtilState.class)
                                    .buildPopup("Select at least one [ Update Flag ].", 0));
                    return;
                } else {
                    updateFlags = 0;
                    if (checkTurns.isChecked()) {
                        updateFlags += CrowdAgentParams.DT_CROWD_ANTICIPATE_TURNS;
                    }

                    if (checkAvoid.isChecked()) {
                        updateFlags += CrowdAgentParams.DT_CROWD_OBSTACLE_AVOIDANCE;
                    }

                    if (checkTopo.isChecked()) {
                        updateFlags += CrowdAgentParams.DT_CROWD_OPTIMIZE_TOPO;
                    }

                    if (checkVis.isChecked()) {
                        updateFlags += CrowdAgentParams.DT_CROWD_OPTIMIZE_VIS;
                    }

                    if (checkSep.isChecked()) {
                        updateFlags += CrowdAgentParams.DT_CROWD_SEPARATION;
                    }
                }

                //Obstacle Avoidance Type. Selection is set to 0 when creating 
                //the listBoxAvoidance so shouldn't need to check for null or
                //parameter configurations less than 0. 
                obstacleAvoidanceType = listBoxAvoidance.getSelectionModel().getSelection();
                
                //Build the params object.
                CrowdAgentParams ap = new CrowdAgentParams();
                ap.radius                   = this.radius;
                ap.height                   = this.height;
                ap.maxAcceleration          = this.max_accel;
                ap.maxSpeed                 = this.max_speed;
                ap.collisionQueryRange      = this.colQueryRange;
                ap.pathOptimizationRange    = this.pathOptimizeRange;
                ap.separationWeight         = this.separationWeight;
                ap.updateFlags              = this.updateFlags;
                ap.obstacleAvoidanceType    = this.obstacleAvoidanceType;
                addAgentsCrowd(ap);
                            
            }

            private void addAgentsCrowd(CrowdAgentParams ap) {
                //anything over 2 arguments creates a new object so split this up.
                LOG.info("<===== Begin AgentSettingsGuiState addAgentsCrowd =====>");
                LOG.info("radius                [{}]", ap.radius);
                LOG.info("height                [{}]", ap.height);
                LOG.info("maxAcceleration       [{}]", ap.maxAcceleration);
                LOG.info("maxSpeed              [{}]", ap.maxSpeed);
                LOG.info("colQueryRange         [{}]", ap.collisionQueryRange);
                LOG.info("pathOptimizationRange [{}]", ap.pathOptimizationRange);
                LOG.info("separationWeight      [{}]", ap.separationWeight);
                LOG.info("updateFlags           [{}]", ap.updateFlags);
                LOG.info("obstacleAvoidanceType [{}]", ap.obstacleAvoidanceType);
                LOG.info("<===== End AgentSettingsGuiState addAgentsCrowd =====>");
            }

        });
    }

    @Override
    protected void cleanup(Application app) {
        //The removal of the gui components is a by product of the removal of 
        //CrowdTabsGuiState where this gui lives.
    }

    @Override
    protected void onEnable() {
        //Called by AgentGridGuiState.
        getStateManager().attach(new CrowdTabsGuiState());
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

    /**
     * @return the contAgentSettingsGui
     */
    public Container getContAgentSettingsGui() {
        return contAgentSettingsGui;
    }
    
    //Explains the agent parameters.
    private void showLegend() {
        String msg = 
                "Agent Radius - The radius of the agent. When presented with an "
                + "opeining they are to large to enter, pathFinding will try to "
                + "navigate around it. [Limit: >= 0]\n\n"
                
                + "Agent Height - The height of the agent. Obstacles with a height "
                + "less than this value will cause pathFinding to try and find "
                + "a navigable path around the obstacle. [Limit: > 0]\n\n"
                
                + "Max Acceleration - When an agent lags behind in the path, this "
                + "is the maximum burst of speed the agent will move at when "
                + "trying to catch up to their expected position. [Limit: >= 0]\n\n"
                
                + "Max Speed - the maximum speed the agent will travel along the "
                + "path when unobstructed. [Limit: >= 0]\n\n"
                           
                + "Collision Query Range - Defines how close a collision element "
                + "must be before it'ss considered for steering behaviors. "
                + "[Limits: > 0]\n\n"
                
                + "Path Optimization Range: The path visibility optimization "
                + "range. [Limit: > 0]\n\n"
                
                + "Separation Weight - How aggresive the agent manager should be "
                + "at avoiding collisions with this agent. [Limit: >= 0]\n\n"
                
                + "Update Flags - Crowd agent update flags. This is a required "
                + "setting.\n\n"
                
                + "Avoidance Type - This is the Obstacle Avoidance configuration "
                + "to be applied to this agent. Currently, the max number of avoidance "
                + "types that can be configured for the Crowd is nine. "
                + "[Limits: 0 <= value <= 8]";
                
        Container window = new Container(new MigLayout("wrap"));
        Label label = window.addChild(new Label(msg));
        label.setMaxWidth(400);
        label.setColor(ColorRGBA.Green);
        window.addChild(new ActionButton(new CallMethodAction("Close", window, "removeFromParent")), "align 50%");
        getState(GuiUtilState.class).centerComp(window);
        //This assures clicking outside of the message should close the window 
        //but not activate underlying UI components.
        GuiGlobals.getInstance().getPopupState().showPopup(window, PopupState.ClickMode.ConsumeAndClose, null, null);
    }
}
