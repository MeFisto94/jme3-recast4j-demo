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
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.ColorRGBA;
import com.jme3.recast4j.Detour.Crowd.Crowd;
import com.jme3.recast4j.Detour.Crowd.Impl.CrowdManagerAppstate;
import com.jme3.recast4j.Detour.Crowd.MovementApplicationType;
import com.jme3.recast4j.demo.layout.MigLayout;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.ActionButton;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.CallMethodAction;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.ListBox;
import com.simsilica.lemur.RollupPanel;
import com.simsilica.lemur.TabbedPanel;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.event.CursorEventControl;
import com.simsilica.lemur.event.DefaultMouseListener;
import com.simsilica.lemur.event.DragHandler;
import com.simsilica.lemur.event.MouseEventControl;
import com.simsilica.lemur.event.PopupState;
import com.simsilica.lemur.text.DocumentModelFilter;
import com.simsilica.lemur.text.TextFilters;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import org.recast4j.detour.NavMesh;
import org.recast4j.detour.NavMeshQuery;
import org.recast4j.detour.crowd.CrowdAgent;
import org.recast4j.detour.crowd.CrowdAgent.CrowdAgentState;
import org.recast4j.detour.crowd.ObstacleAvoidanceQuery.ObstacleAvoidanceParams;
import org.recast4j.detour.io.MeshSetReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Robert
 */
@SuppressWarnings("unchecked")
public class CrowdState extends BaseAppState {

    private static final Logger LOG = LoggerFactory.getLogger(CrowdState.class.getName());

    private Container contTabs;
    private DocumentModelFilter doc;
    private TextField fieldNavMesh;
    private ListBox listBoxAvoidance;
    private TextField fieldVelocityBias;
    private TextField fieldAdaptiveRings;
    private TextField fieldAdaptiveDivs;
    private TextField fieldAdaptiveDepth;
    private ListBox listMoveType;
    private float maxPopupSize;
    private TextField fieldMaxAgents;
    private TextField fieldMaxAgentRadius;
    private ListBox listActiveCrowds;
    private TextField fieldCrowdName;
    private HashMap<String, NavMeshQuery> mapCrowds;
    
    
    @Override
    protected void initialize(Application app) {
        maxPopupSize = 800.0f;
        mapCrowds = new HashMap();
    }

    @Override
    protected void cleanup(Application app) {
        //Removing will also cleanup the AgentGridState and AgentParamState
        //lemur objects.
        ((SimpleApplication) getApplication()).getGuiNode().detachChild(contTabs);
    }

    /**
     * Called by AgentParamState(onEnable). CrowdState needs AgentGridState and 
     * AgentParamState to build its gui. This is the end of the attachment chain. 
     * AgentGridState(onEnable)=>AgentParamState(onEnable)=>CrowdState(onEnable)
     */
    @Override
    protected void onEnable() {
        
        //The top container for the crowd builder panel.
        Container contCrowd = new Container(new MigLayout("align center"));
        contCrowd.setName("CrowdState contBuildCrowdGui");
        contCrowd.setAlpha(0, false);
        
        
        
        //Container that holds the parameters for starting the crowd.
        Container contCrowdParam = new Container(new MigLayout("wrap", "[grow]"));
        contCrowdParam.setName("CrowdState contCrowdParam");
        contCrowdParam.setAlpha(0, false);
        contCrowd.addChild(contCrowdParam, "growx, growy"); 
        
        //Start Crowd parameters
        //The navmesh loader. 
        contCrowdParam.addChild(new Label("Crowd Parameters"));
        contCrowdParam.addChild(new Label("NavMesh"), "split 2"); 
        fieldNavMesh = contCrowdParam.addChild(new TextField("test.nm"), "growx");
        fieldNavMesh.setSingleLine(true);
        
        //The navmesh loader. 
        contCrowdParam.addChild(new Label("Crowd Name"), "split 2"); 
        fieldCrowdName = contCrowdParam.addChild(new TextField("Crowd"), "growx");
        fieldCrowdName.setSingleLine(true);
        
        //Max agents for the crowd.
        contCrowdParam.addChild(new Label("Max Agents"), "split 2, growx"); 
        doc = new DocumentModelFilter();
        doc.setInputTransform(TextFilters.numeric());
        doc.setText("100");
        fieldMaxAgents = contCrowdParam.addChild(new TextField(doc));
        fieldMaxAgents.setSingleLine(true);
        fieldMaxAgents.setPreferredWidth(50);
        
        //Max agent radius for an agent in the crowd.
        contCrowdParam.addChild(new Label("Max Agent Radius"), "split 2, growx");
        fieldMaxAgentRadius = contCrowdParam.addChild(new TextField("0.6"));
        fieldMaxAgentRadius.setSingleLine(true);
        fieldMaxAgentRadius.setPreferredWidth(50);                    
  
        
        
        //Container for list movement.
        Container contListMoveType = new Container(new MigLayout("wrap"));
        contListMoveType.setName("CrowdState contListMoveType");
        contListMoveType.setAlpha(0, false);
        contCrowd.addChild(contListMoveType, "top");
        
        //Movement types for the crowd.
        contListMoveType.addChild(new Label("Movement Type"));
        //Movement types for crowd
        listMoveType = contListMoveType.addChild(new ListBox());
        listMoveType.getSelectionModel().setSelection(0);
        listMoveType.getModel().add("BETTER_CHARACTER_CONTROL");
        listMoveType.getModel().add("DIRECT");
        listMoveType.getModel().add("CUSTOM");
        listMoveType.getModel().add("NONE");
        
        
        
        //Container that holds to Active Crowds.
        Container contActiveCrowds = new Container(new MigLayout("wrap"));
        contActiveCrowds.setName("CrowdState contActiveCrowds");
        contActiveCrowds.setAlpha(0, false);
        contCrowd.addChild(contActiveCrowds, "wrap, flowy, growx, growy");
        
        contActiveCrowds.addChild(new Label("Active Crowds"));
        listActiveCrowds = contActiveCrowds.addChild(new ListBox(), "growx, growy");  
        //Button to stop the Crowd.
        contActiveCrowds.addChild(new ActionButton(new CallMethodAction("Shutdown Crowd", this, "shutdown")), "top");
        
        
        
        //Give this its own row so can can align parameters with the listBox
        Container contAvoidLabel = new Container(new MigLayout(null));
        contAvoidLabel.setName("CrowdState contAvoidLabel");
        contAvoidLabel.setAlpha(0, false);
        contAvoidLabel.addChild(new Label("Obstacle Avoidance Parameters")); 
        contCrowd.addChild(contAvoidLabel, "wrap");
        
        //Container that holds the obstacle avoidance parameters for the crowd 
        //agents.
        Container contAvoidance = new Container(new MigLayout("wrap", "[grow]"));
        contAvoidance.setName("CrowdState contAvoidance");
        contAvoidance.setAlpha(0, false);
        contCrowd.addChild(contAvoidance, "growx, growy");
                
        //Velocity Bias.
        contAvoidance.addChild(new Label("Velocity Bias"), "split 2, growx"); 
        fieldVelocityBias = contAvoidance.addChild(new TextField("0.4"));
        fieldVelocityBias.setSingleLine(true);
        fieldVelocityBias.setPreferredWidth(50);
        
        //Adaptive Divisions.
        contAvoidance.addChild(new Label("Adaptive Divisions"), "split 2, growx"); 
        doc = new DocumentModelFilter();
        doc.setInputTransform(TextFilters.numeric());
        doc.setText("7");
        fieldAdaptiveDivs = contAvoidance.addChild(new TextField(doc));
        fieldAdaptiveDivs.setSingleLine(true);
        fieldAdaptiveDivs.setPreferredWidth(50);
        
        //Adaptive Rings.
        contAvoidance.addChild(new Label("Adaptive Rings"), "split 2, growx"); 
        doc = new DocumentModelFilter();
        doc.setInputTransform(TextFilters.numeric());
        doc.setText("2");
        fieldAdaptiveRings = contAvoidance.addChild(new TextField(doc));
        fieldAdaptiveRings.setSingleLine(true);
        fieldAdaptiveRings.setPreferredWidth(50);
        
        //Adaptive Rings.
        contAvoidance.addChild(new Label("Adaptive Depth"), "split 2, growx"); 
        doc = new DocumentModelFilter();
        doc.setInputTransform(TextFilters.numeric());
        doc.setText("5");
        fieldAdaptiveDepth = contAvoidance.addChild(new TextField(doc));
        fieldAdaptiveDepth.setSingleLine(true);
        fieldAdaptiveDepth.setPreferredWidth(50);
        
        
        
        //Holds the listbox for avoidance parameters.
        Container contListBoxAvoidance = new Container(new MigLayout("wrap", "[grow]"));
        contListBoxAvoidance.setName("CrowdState contListBoxAvoidance");
        contListBoxAvoidance.setAlpha(0, false);
        contCrowd.addChild(contListBoxAvoidance, "wrap, growx");
        
        //Parameters list.
        listBoxAvoidance = contListBoxAvoidance.addChild(new ListBox(), "growx"); 
        listBoxAvoidance.setVisibleItems(3);
        
        //The ObstacleAvoidanceParams string for the listbox.      
        for (int i = 0; i < 8; i++) {
            String params = "<=====    " + i + "    =====>\n"  
                + "velBias                = 0.4\n"
                + "adaptiveDivs       = 7\n"
                + "adaptiveRings     = 2\n"
                + "adaptiveDepth    = 5";
            listBoxAvoidance.getModel().add(params);
        }
        listBoxAvoidance.getSelectionModel().setSelection(0);
        
        //Update a parameter button.
        Button butParam = contListBoxAvoidance.addChild(new Button("Update Parameter"));
        
        /**
         * Sets the listbox text and creates a new ObstacleAvoidanceParams object.
         * Parameters are case sensitive and are as follows:
         *          'b' sets the Velocity Bias
         *          'd' sets the Adaptive Divisions
         *          'D' sets the Adaptive Depth
         *          'g' sets the Grid Size
         *          'h' sets the Horizon Time
         *          'i' sets the Weight To Impact
         *          'r' sets the Adaptive Rings
         *          's' sets the Weight Side
         *          'v' sets the Weight Desired Velocity
         *          'V' sets the Weight Current Velocity
         */
        MouseEventControl.addListenersToSpatial(butParam, new DefaultMouseListener() {
            @Override
            protected void click( MouseButtonEvent event, Spatial target, Spatial capture ) {                
                
                String params = "<=====    " + listBoxAvoidance.getSelectionModel().getSelection() + "    =====>\n"  
                        + "velBias                = $b\n"
                        + "adaptiveDivs       = $d\n"
                        + "adaptiveRings     = $r\n"
                        + "adaptiveDepth    = $D";
                updateParam(params);
            }
        }); 
        
//        //Give this its own row so can align the listBoxes.
//        Container contActiveLabel = new Container(new MigLayout(null));
//        contActiveLabel.setName("CrowdState contActiveLabel");
//        contActiveLabel.setAlpha(0, false);
//        contActiveLabel.addChild(new Label("Active Crowds"));
//        contCrowd.addChild(contActiveLabel, "wrap");
        
       
        
        //Holds the Legend and Setup buttons.
        Container contButton = new Container(new MigLayout(null, // Layout Constraints
                "[]push[][]")); // Column constraints [min][pref][max]
        contButton.setName("CrowdState contButton");
        contButton.setAlpha(1, false);
        contCrowd.addChild(contButton, "growx, span 2"); //cell col row span w h
        
        //Legend
        contButton.addChild(new ActionButton(new CallMethodAction("Legend", this, "showLegend")));
        //Button to start the Crowd.
        contButton.addChild(new ActionButton(new CallMethodAction("Start Crowd", this, "startCrowd")));
        
        //Create the container that will hold the tab panel for BuildGridGui and 
        //BuildParamGui gui.
        contTabs = new Container(new MigLayout("wrap"));
        contTabs.setName("CrowdState contTabs");
        //Make it dragable.
        DragHandler dragHandler = new DragHandler();
        CursorEventControl.addListenersToSpatial(contTabs, dragHandler);
        contTabs.addChild(new Label("Crowd Builder"));
        
        //Create the tabbed panel.
        TabbedPanel tabPanel = contTabs.addChild(new TabbedPanel());
        
        //Modify tabs to stretch collapse button. MigLayout(null, "[grow, fill]"))
        //Add a rollup panel for the crowd settings panel.
        RollupPanel rollCrowd = new RollupPanel("Expand / Collapse", 
                contCrowd, "glass");
        rollCrowd.getTitleContainer().setLayout(new MigLayout(null, "[grow, fill]"));
        rollCrowd.getTitleElement().removeFromParent();
        rollCrowd.getTitleContainer().addChild(rollCrowd.getTitleElement());
        rollCrowd.getTitleElement().setTextHAlignment(HAlignment.Center);
        rollCrowd.setAlpha(0, false); 
        rollCrowd.setOpen(false);
        tabPanel.addTab("Crowd", rollCrowd);
        
        //Add a rollup panel so can hide agent grid.
        RollupPanel rollAgentGrid = new RollupPanel("Expand / Collapse", 
                getState(AgentGridState.class).getContAgentGrid(), "glass");
        rollAgentGrid.getTitleContainer().setLayout(new MigLayout(null, "[grow, fill]"));
        rollAgentGrid.getTitleElement().removeFromParent();
        rollAgentGrid.getTitleContainer().addChild(rollAgentGrid.getTitleElement());
        rollAgentGrid.getTitleElement().setTextHAlignment(HAlignment.Center);
        rollAgentGrid.setAlpha(0, false);
        rollAgentGrid.setOpen(false);
        tabPanel.addTab("Agent Grid", rollAgentGrid);
        
        //Add a rollup panel so can hide agent parameters.
        RollupPanel rollAgentParam = new RollupPanel("Expand / Collapse", 
                getState(AgentParamState.class).getContAgentParams(), "glass");
        rollAgentParam.getTitleContainer().setLayout(new MigLayout(null, "[grow, fill]"));
        rollAgentParam.getTitleElement().removeFromParent();
        rollAgentParam.getTitleContainer().addChild(rollAgentParam.getTitleElement());
        rollAgentParam.getTitleElement().setTextHAlignment(HAlignment.Center);
        rollAgentParam.setAlpha(0, false); 
        rollAgentParam.setOpen(false);
        tabPanel.addTab("Agent Parameters", rollAgentParam);
        
        getState(GuiUtilState.class).centerComp(contTabs);
        
        ((SimpleApplication) getApplication()).getGuiNode().attachChild(contTabs);
    }

    /**
     * Called by the DemoApplication F1 button ActionListener as part of a chain 
     * detachment of states. This is the start of the detachment chain.
     * CrowdState(onDisable)=>AgentParamState(onDisable)=>AgentGridState(onDisable)
     */
    @Override
    protected void onDisable() {
        if (getStateManager().hasState(getState(AgentParamState.class))) {
            getStateManager().getState(AgentParamState.class).setEnabled(false);
        }
        getStateManager().detach(this);
    }
    
    @Override
    public void update(float tpf) {
//        if (getState(CrowdManagerAppstate.class).getCrowdManager().getNumberOfCrowds() > 0 ) {
//            int numberOfCrowds = getState(CrowdManagerAppstate.class).getCrowdManager().getNumberOfCrowds();
//            for (int i = 0; i < numberOfCrowds; i++) {
//                dumpActiveAgents(i);
//            }
//        }
    }
    
    //Explains the agent parameters.
    private void showLegend() {
        String msg = 
                "NavMesh - The navigation mesh to use for planning.\n\n" + 
                
                "Crowd Name - The name for this crowd. Used for adding agents to" + 
                "a crowd. Each crowd must have a unique name. Spaces count in " + 
                "the naming so if there is added space after a crowd name, that " + 
                "crowd  will be considered unique.\n\n" +
                
                "Max Agents - The maximum number of agents the crowd can manage. " + 
                "[Limit: >= 1]\n\n" +
                
                "Max Agent Radius - The maximum radius of any agent that will be " + 
                "added to the crowd. [Limit: > 0]\n\n" + 
                
                "Movement Type - Each type determines how movement is applied to " + 
                "an agent.\n\n" + 
                
                "BETTER_CHARACTER_CONTROL - As the name implies, uses physics " +
                "and the BetterCharacterControl to set move and view direction.\n\n" +

                "DIRECT - Direct setting of the agents translation. No controls " + 
                "are needed for movement but you must set the view direction yourself.\n\n" +
                
                "CUSTOM - With custom, you implement the applyMovement() method " + 
                "from the ApplyFunction interface. This will give you access to " + 
                "the agents CrowdAgent object, the new position and the velocity " + 
                "of the agent.\n\n" + 
                
                "NONE - No movement is implemented. You can freely interact with" + 
                "the crowd and implement your own movement soloutions.\n\n" +

                "Active Crowds - The list of active crowds. A crowd must be " + 
                "selected before any agents can be added to a crowd or targets " + 
                "for agents can be set.\n\n" + 
                
                "Obstacle Avoidance Parameters - The shared avoidance " + 
                "configuration for an Agent inside the crowd. When first " + 
                "instantiating the crowd, you are alloted eight parameter " + 
                "objects in total. All eight slots are pre-filled with " + 
                "the defaults listed below.\n\n" + 
                
                "[Defaults]\n" +
                "velBias = 0.4f\n" +
                "weightDesVel = 2.0f\n" +
                "weightCurVel = 0.75f\n" +
                "weightSide = 0.75f\n" +
                "weightToi = 2.5f\n" +
                "horizTime = 2.5f\n" +
		"gridSize = 33\n" +
                "adaptiveDivs = 7\n" +
                "adaptiveRings = 2\n" +
                "adaptiveDepth = 5\n\n" +
                
                "You may not remove any default parameter from the Crowd, " + 
                "however, you can overwrite them. To change any parameter, first " + 
                "set the parameters you desire in the Obstacle Avoidance " + 
                "Parameters section, then select any parameter from the list and " + 
                "click the [Update Parameter] button.\n\n" +
                
                "Velocity Bias - The velocity bias describes how the sampling " + 
                "patterns is offset from the (0,0) based on the desired velocity. " + 
                "This allows to tighten the sampling area and cull a lot of " + 
                "samples. [Limit: 0-1]\n\n" +
                
                "Weight Desired Velocity - How much deviation from desired " + 
                "velocity is penalized, the more penalty applied to this, the " + 
                "more \"goal oriented\" the avoidance is, at the cost of " + 
                "getting more easily stuck at local minimas.\n\n" +
                
                "Weight Current Velocity - How much deviation from current " + 
                "velocity is penalized, the more penalty applied to this, the " + 
                "more stubborn the agent is. This basically is a low pass " + 
                "filter, and very important part of making things work.\n\n" +
                
                "Weight Side - In order to avoid reciprocal dance, the agenst " + 
                "prefer to pass from right, this weight applies penalty to " + 
                "velocities which try to take over from the wrong side.\n\n" +
                
                "Weight To Impact - How much penalty is added based on time to " + 
                "impact. Too much penalty and the agents are shy, too little " + 
                "and they avoid too late.\n\n" +
                
                "Horrizon Time - Time horizon, this affects how early the agents " + 
                "start to avoid each other. Too long horizon and the agents are " + 
                "scared of going through tight spots, and too small and they " + 
                "avoid too late (closely related to weightToi).\n\n" +
                
                "Adaptive Divisions - Number of divisions per ring. [Limit: 1-32]\n\n" +
                
                "Adaptive Rings - Number of rings. [Limit: 1-4]\n\n" +
                
                "Adaptive Depth - Number of iterations at best velocity.";
                
        Container window = new Container(new MigLayout("wrap"));
        Label label = window.addChild(new Label(msg));
        label.setMaxWidth(getApplication().getCamera().getWidth());
        label.setColor(ColorRGBA.Green);
        window.addChild(new ActionButton(new CallMethodAction("Close", window, "removeFromParent")), "align 50%");
        getState(GuiUtilState.class).centerComp(window);
        //This assures clicking outside of the message should close the window 
        //but not activate underlying UI components.
        GuiGlobals.getInstance().getPopupState().showPopup(window, PopupState.ClickMode.ConsumeAndClose, null, null);
    }
    
    private void shutdown() {
        
        //Get the Crowd from selectedCrowd.
        int selectedCrowd = getSelectedCrowd();
        
        //Check to make sure the crowd has been selected.
        if (selectedCrowd == -1) {
            GuiGlobals.getInstance().getPopupState()
                    .showModalPopup(getState(GuiUtilState.class)
                            .buildPopup("You must select a crowd from the "
                                    + "[ Active Crowds ] list.", 0));
            return;
        }
        
        //Get the crowds name from the listActiveCrowds selectedParam.
        String crowdName = listActiveCrowds.getModel().get(selectedCrowd).toString();

        //We check mapGrids to see if the key exists. If not, go no further.
        if (!mapCrowds.containsKey(crowdName)) {
            GuiGlobals.getInstance().getPopupState()
                    .showModalPopup(getState(GuiUtilState.class)
                            .buildPopup("No crowd found by that name.", 0));
            return;
        }
        
        //We have a valid crowd so remove it from the map, the CrowdManager and 
        //the listActiveCrowds listbox.
        Iterator<String> iterator = mapCrowds.keySet().iterator();
        while (iterator.hasNext()) {
            String entry = iterator.next();
            if (entry.equals(crowdName)) {
                //To fully remove the crowd we have to remove it from the 
                //CrowdManager, mapCrowds (removes the query object also), and 
                //the listActiveCrowds.
                Crowd crowd = getState(CrowdManagerAppstate.class).getCrowdManager().getCrowd(selectedCrowd);
                getState(CrowdManagerAppstate.class).getCrowdManager().removeCrowd(crowd);
                remove(listActiveCrowds, selectedCrowd);
                iterator.remove();
                break;
            }
        }    
    }
    
    /**
     * Starts and adds a crowd to the CrowdManager.
     */
    private void startCrowd() {
        int maxAgents;
        float maxAgentRadius;
        String mesh;
        String crowdName;

        //The name of the mesh to load.                
        if (fieldNavMesh.getText().isEmpty()) {
            GuiGlobals.getInstance().getPopupState()
                    .showModalPopup(getState(GuiUtilState.class)
                            .buildPopup("You must enter a [ NavMesh ] name.", 0));
            return;
        } else {
            mesh = fieldNavMesh.getText();
        }
        
        
        //The name of this crowd.                
        if (fieldCrowdName.getText().isEmpty()) {
            GuiGlobals.getInstance().getPopupState()
                    .showModalPopup(getState(GuiUtilState.class)
                            .buildPopup("You must enter a [ Crowd ] name.", 0));
            return;
        } else if (mapCrowds.containsKey(fieldCrowdName.getText())) {
            GuiGlobals.getInstance().getPopupState()
                    .showModalPopup(getState(GuiUtilState.class)
                            .buildPopup("[ " 
                                    + fieldCrowdName.getText() 
                                    + " ] has already been activated. "
                                    + "Change the crowd name or remove the existing crowd before proceeding.", maxPopupSize));
            return;
        } else {
            crowdName = fieldCrowdName.getText();
        }
        
        //The max agents for the crowd. Uses numeric doc filter to prevent bad data.
        if (fieldMaxAgents.getText().isEmpty()) {
            GuiGlobals.getInstance().getPopupState()
                    .showModalPopup(getState(GuiUtilState.class)
                            .buildPopup("[ Max Agents ] requires a valid int value.", 0));
            return;
        } else {
            maxAgents = new Integer(fieldMaxAgents.getText());
            //Stop useless input.
            if (maxAgents < 1) {
                GuiGlobals.getInstance().getPopupState()
                        .showModalPopup(getState(GuiUtilState.class)
                                .buildPopup("[ Max Agents ] requires a int value >= 1", 0));
                return;
            }
        }
        
        //The max agent radius for an agent in the crowd.
        if (!getState(GuiUtilState.class).isNumeric(fieldMaxAgentRadius.getText()) 
        ||  fieldMaxAgentRadius.getText().isEmpty()) {
            GuiGlobals.getInstance().getPopupState()
                    .showModalPopup(getState(GuiUtilState.class)
                            .buildPopup("[ Max Agent Radius ] requires a valid float value.", 0));
            return;
        } else {
            maxAgentRadius = new Float(fieldMaxAgentRadius.getText());
            //Stop negative input.
            if (maxAgentRadius <= 0.0f ) {
                GuiGlobals.getInstance().getPopupState()
                        .showModalPopup(getState(GuiUtilState.class)
                                .buildPopup("[ Max Agent Radius ] requires a float value between > 0.", 0));
                return;
            }
        }
        
        MovementApplicationType applicationType = null;
        
        switch (listMoveType.getSelectionModel().getSelection()) {
            case 0:
                applicationType = MovementApplicationType.BETTER_CHARACTER_CONTROL;
                break;
            case 1: 
                applicationType = MovementApplicationType.DIRECT;
                break;
            case 2:
                applicationType = MovementApplicationType.CUSTOM;
                break;
            case 3:
                applicationType = MovementApplicationType.NONE;
        }
        
        MeshSetReader msr = new MeshSetReader();
        try {
            //Read in the saved navMesh with same maxVertPerPoly(3) saved. Will 
            //be added to mapCrowds as a key using the text returned by 
            //fieldCrowdName.
            NavMesh navMesh = msr.read(new FileInputStream(mesh), 3);
            //Create the query object for pathfinding in this Crowd. Will be 
            //added to the mapCrowds as a value so each crowd query object is
            //referenced.  
            NavMeshQuery query = new NavMeshQuery(navMesh);
            
            Crowd crowd = new Crowd(applicationType, maxAgents, maxAgentRadius, navMesh);
            //Add to CrowdManager, mapCrowds, and listActiveCrowds.
            getState(CrowdManagerAppstate.class).getCrowdManager().addCrowd(crowd);      
            mapCrowds.put(crowdName, query);
            listActiveCrowds.getModel().add(crowdName);  
            
        } catch (IOException ex) {
            LOG.info("{} {}", CrowdState.class.getName(), ex);
        }

    }
    
    /**
     * Takes a string and looks for the token '$' to format a new string based 
     * on the char following the token. Builds the new ObstacleAvoidanceParams 
     * object for the selected Crowd and inserts it into the appropriate slot. 
     * It is expected the Crowd is running.
     * 
     * @param format The string to be formatted.
     */
    private void updateParam(String format) {
        float velBias;
        int adaptiveDivs;
        int adaptiveDepth;
        int adaptiveRings;
        Integer selectedParam;
        int selectedCrowd;
                
        //The velocity bias settings.
        if (!getState(GuiUtilState.class).isNumeric(fieldVelocityBias.getText()) 
        ||  fieldVelocityBias.getText().isEmpty()) {
            GuiGlobals.getInstance().getPopupState()
                    .showModalPopup(getState(GuiUtilState.class)
                            .buildPopup("[ Velocity Bias ] requires a valid float value.", 0));
            return;
        } else {
            velBias = new Float(fieldVelocityBias.getText());
            //Stop negative input.
            if (velBias < 0.0f || velBias > 1) {
                GuiGlobals.getInstance().getPopupState()
                        .showModalPopup(getState(GuiUtilState.class)
                                .buildPopup("[ Velocity Bias ] requires a float value between 0 and 1 inclusive.", 0));
                return;
            }
        }
        
        //The adaptive divisions settings. Uses numeric doc filter to prevent
        //bad data.
        if (fieldAdaptiveDivs.getText().isEmpty()) {
            GuiGlobals.getInstance().getPopupState()
                    .showModalPopup(getState(GuiUtilState.class)
                            .buildPopup("[ Adaptive Divisions ] requires a valid int value.", 0));
            return;
        } else {
            adaptiveDivs = new Integer(fieldAdaptiveDivs.getText());
            //Stop useless input.
            if (adaptiveDivs < 1 || adaptiveDivs > 32) {
                GuiGlobals.getInstance().getPopupState()
                        .showModalPopup(getState(GuiUtilState.class)
                                .buildPopup("[ Adaptive Divisions ] requires a int value between 1 and 32 inclusive.", 0));
                return;
            }
        }
        
         //The adaptive depth settings. Uses numeric doc filter to prevent bad data.
        if (fieldAdaptiveDepth.getText().isEmpty()) {
            GuiGlobals.getInstance().getPopupState()
                    .showModalPopup(getState(GuiUtilState.class)
                            .buildPopup("[ Adaptive Divisions ] requires a valid int value.", 0));
            return;
        } else {
            adaptiveDepth = new Integer(fieldAdaptiveDepth.getText());
            //Stop useless input.
            if (adaptiveDepth < 1 ) {
                GuiGlobals.getInstance().getPopupState()
                        .showModalPopup(getState(GuiUtilState.class)
                                .buildPopup("[ Adaptive Depth ] requires a int value >= 1.", 0));
                return;
            }
        }
        
        //The adaptive depth settings. Uses numeric doc filter to prevent bad data.
        if (fieldAdaptiveRings.getText().isEmpty()) {
            GuiGlobals.getInstance().getPopupState()
                    .showModalPopup(getState(GuiUtilState.class)
                            .buildPopup("[ Adaptive Rings ] requires a valid int value.", 0));
            return;
        } else {
            adaptiveRings = new Integer(fieldAdaptiveRings.getText());
            //Stop negative input.
            if (adaptiveRings < 1 || adaptiveRings > 4) {
                GuiGlobals.getInstance().getPopupState()
                        .showModalPopup(getState(GuiUtilState.class)
                                .buildPopup("[ Adaptive Rings ] requires a int value between 1 and 4 inclusive.", 0));
                return;
            }
        }
        
        //Get the selectedParam from listBoxAvoidance.
        selectedParam = listBoxAvoidance.getSelectionModel().getSelection();
        
        //Check to make sure a an avoidance parmeter has been selected.
        if (selectedParam == null) {
            GuiGlobals.getInstance().getPopupState()
                    .showModalPopup(getState(GuiUtilState.class)
                            .buildPopup("You must select a [ Parameter ] from the list before it can be updated.", 0));
            return;
        }
        
        //Get the crowd from listActiveCrowds.
        selectedCrowd = this.getSelectedCrowd();
        
        //Check to make sure a crowd has been selected.
        if (selectedCrowd == -1) {
            GuiGlobals.getInstance().getPopupState()
                    .showModalPopup(getState(GuiUtilState.class)
                            .buildPopup("You must select a [ Active Crowd ] "
                                    + "from the list before a parameter can be updated.", 0));
            return;
        }
        
        LOG.info("<========== Beign CrowdState updateParam ==========>");

        //If run into threading problems use StringBuffer.
        StringBuilder buf               = new StringBuilder();
        String str                      = format;
        String i                        = null;
        ObstacleAvoidanceParams params  = new ObstacleAvoidanceParams();
        
        for ( int j = 0; j < str.length(); j++ ) {
            if ( str.charAt(j) != '$' ) {
                buf.append(str.charAt(j));
                continue;
            }
            
            char charAt = str.charAt(++j);
            
            switch (charAt) {
                case 'b': //sets the Velocity Bias
                    i = fieldVelocityBias.getText(); 
                    params.velBias = velBias;
                    LOG.info("velBias       [{}]", params.velBias);
                    break;
                case 'd': //sets the Adaptive Divisions
                    i = this.fieldAdaptiveDivs.getText();
                    params.adaptiveDivs = adaptiveDivs;
                    LOG.info("adaptiveDivs  [{}]", params.adaptiveDivs);
                    break;
                case 'D': //sets the Adaptive Depth
                    i = this.fieldAdaptiveDepth.getText();
                    params.adaptiveDepth = adaptiveDepth;
                    LOG.info("adaptiveRings [{}]", params.adaptiveDepth);
                    break;
                case 'g': //sets the Grid Size
                    LOG.info("gridSize [{}]");
                    break;
                case 'h': //sets the Horizon Time
                    LOG.info("horizTime [{}]");
                    break;
                case 'i': //sets the Weight To Impact
                    LOG.info("weightToi [{}]");
                    break;
                case 'r': //sets the Adaptive Rings
                    i = this.fieldAdaptiveRings.getText();
                    params.adaptiveRings = adaptiveRings;
                    LOG.info("adaptiveDepth [{}]", params.adaptiveRings);
                    break;
                case 's': //sets the Weight Side
                    LOG.info("weightSide [{}]");
                    break;
                case 'v': //sets the Weight Desired Velocity
                    LOG.info("weightDesVel [{}]");
                    break;
                case 'V': //sets the Weight Current Velocity
                    LOG.info("weightCurVel [{}]");
                    break;
            }
            buf.append(i);
        }        
        
        //Remove selected parameter.
        remove(listBoxAvoidance, selectedParam);
        //Insert the new parameters into the list.
        insert(listBoxAvoidance, selectedParam, buf.toString());
        //Inject the new parameter into the crowd.
        getState(CrowdManagerAppstate.class).getCrowdManager().getCrowd(selectedCrowd)
                .setObstacleAvoidanceParams(selectedParam, params);
        
        //Check the data, remove when ready.
        ObstacleAvoidanceParams oap = getState(CrowdManagerAppstate.class)
                .getCrowdManager().getCrowd(selectedCrowd).getObstacleAvoidanceParams(selectedParam);
        //Check the data, remove when ready.
        int agentCount = getState(CrowdManagerAppstate.class).getCrowdManager()
                .getCrowd(selectedCrowd).getAgentCount();
        
        //Remove when ready.
        LOG.info("CrowdState ObsAvParam#        [{}]", selectedParam);
        LOG.info("CrowdState Crowd#             [{}]", selectedCrowd);
        LOG.info("CrowdManager agentCount       [{}]", agentCount);
        LOG.info("CrowdManager velBias          [{}]", oap.velBias);
        LOG.info("CrowdManager adaptiveDivs     [{}]", oap.adaptiveDivs);
        LOG.info("CrowdManager adaptiveDepth    [{}]", oap.adaptiveDepth);
        LOG.info("CrowdManager adaptiveRings    [{}]", oap.adaptiveRings);
        LOG.info("<========== End CrowdState updateParam ==========>");

    }
    
    //Insert a parameter string into the List.
    protected void insert(ListBox list, int idx, String txt) {
        list.getModel().add(idx, txt);
        list.getSelectionModel().setSelection(idx);
    }
    
    //Remove parameter string from a listBox.
    protected void remove(ListBox list, int idx) {
        list.getModel().remove(idx);
    }
    
    /**
     * Gets the currently selected crowd from the list of active crowds.
     * 
     * @return The active crowd or -1 if no active crowd was selected.
     */
    public int getSelectedCrowd() {
        Integer selectedCrowd = listActiveCrowds.getSelectionModel().getSelection();
        if (selectedCrowd == null) {
            return -1;
        } else {
            return selectedCrowd;
        }
    }
    
    /**
     * Gets the query object for any selected crowd.
     * 
     * @return The query object for a selected crowd or null if the crowd has 
     * not been selected in the Active Crowds list or if the query object doesn't
     * exist in the mapCrowds list.
     */
    public NavMeshQuery getQuery() {
        int selectedCrowd = getSelectedCrowd();
        
        //Check to make sure a crowd has been selected.
        if (selectedCrowd == -1) {
            return null;
        } 
        
        String crowd = listActiveCrowds.getModel().get(selectedCrowd).toString();
        
        //Make sure the crowd selected exits in the map.
        if (!mapCrowds.containsKey(crowd)) { 
           return null;
        }
        
        return mapCrowds.get(crowd);
    }
    
    protected void dumpActiveAgents(int i) {
        Crowd crowd = getState(CrowdManagerAppstate.class).getCrowdManager().getCrowd(i);
        
        for (CrowdAgent ag : crowd.getActiveAgents()) {
            if (ag.isActive()) {
                LOG.info("Crowd [{}] Active Agents [{}]", i, crowd.getActiveAgents().size());
                LOG.info("State [{}]", ag.state);
                LOG.info("Pos [{},{},{}]",ag.npos[0], ag.npos[1], ag.npos[2]);
            }
        }
    }
    
}
