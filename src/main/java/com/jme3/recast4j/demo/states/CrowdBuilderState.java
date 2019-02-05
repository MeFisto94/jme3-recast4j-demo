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
import com.jme3.math.Vector3f;
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
import com.simsilica.lemur.core.VersionedReference;
import com.simsilica.lemur.event.CursorEventControl;
import com.simsilica.lemur.event.DefaultMouseListener;
import com.simsilica.lemur.event.DragHandler;
import com.simsilica.lemur.event.MouseEventControl;
import com.simsilica.lemur.event.PopupState;
import com.simsilica.lemur.text.DocumentModelFilter;
import com.simsilica.lemur.text.TextFilters;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.recast4j.detour.NavMesh;
import org.recast4j.detour.NavMeshQuery;
import org.recast4j.detour.crowd.CrowdAgent;
import org.recast4j.detour.crowd.ObstacleAvoidanceQuery.ObstacleAvoidanceParams;
import org.recast4j.detour.io.MeshSetReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Robert
 */
@SuppressWarnings("unchecked")
public class CrowdBuilderState extends BaseAppState {

    private static final Logger LOG = LoggerFactory.getLogger(CrowdBuilderState.class.getName());

    private Container contTabs;
    private DocumentModelFilter doc;
    private TextField fieldNavMesh;
    private TextField fieldMaxAgents;
    private TextField fieldMaxAgentRadius;
    private TextField fieldCrowdName;
    private TextField fieldVelocityBias;
    private TextField fieldWeightDesVel;
    private TextField fieldWeightCurVel;
    private TextField fieldWeightSide;
    private TextField fieldWeightToi;
    private TextField fieldHorizTime;
    private TextField fieldGridSize;
    private TextField fieldAdaptiveRings;
    private TextField fieldAdaptiveDivs;
    private TextField fieldAdaptiveDepth;    
    private ListBox<String> listBoxAvoidance;    
    private ListBox<String> listMoveType;
    private ListBox<String> listActiveCrowds;
    private HashMap<String, CrowdNQuery> mapCrowds;
    // Keep tracking crowdName selected
    private VersionedReference<Set<Integer>> selectionRef; 
    // Keep tracking crowdName selected
    private VersionedReference<List<String>> modelRef; 
    private String defaultOAP;

    
    @Override
    protected void initialize(Application app) {
        mapCrowds = new HashMap();
        defaultOAP =                
                  "velBias              = n\\a\n"
                + "weightDesVel  = n\\a\n"
                + "weightCurVel   = n\\a\n"
                + "weightSide       = n\\a\n"
                + "weightToi         = n\\a\n"
                + "horizTime         = n\\a\n"
                + "gridSize            = n\\a\n"
                + "adaptiveDivs    = n\\a\n"               
                + "adaptiveRings  = n\\a\n"
                + "adaptiveDepth = n\\a";
    }

    @Override
    protected void cleanup(Application app) {
        //Removing will also cleanup the AgentGridState and AgentParamState
        //lemur objects.
        ((SimpleApplication) getApplication()).getGuiNode().detachChild(contTabs);

        Iterator<Map.Entry<String, CrowdNQuery>> iterator = mapCrowds.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CrowdNQuery> entry = iterator.next();
            Crowd crowd = entry.getValue().getCrowd();
            getState(CrowdManagerAppstate.class).getCrowdManager().removeCrowd(crowd);
            iterator.remove();
        }
    }

    /**
     * Called by AgentParamState(onEnable). CrowdBuilderState needs 
     * AgentGridState and AgentParamState to build its gui. This is the end of 
     * the attachment chain. 
     * AgentGridState(onEnable)=>AgentParamState(onEnable)=>CrowdBuilderState(onEnable)
     */
    @Override
    protected void onEnable() {
        
        //The top container for the crowd builder panel.
        Container contCrowd = new Container(new MigLayout("align center"));
        contCrowd.setName("CrowdBuilderState contCrowd");
        contCrowd.setAlpha(0, false);
        
        
        
        //Container that holds the parameters for starting the crowd.
        Container contCrowdParam = new Container(new MigLayout("wrap", "[grow]"));
        contCrowdParam.setName("CrowdBuilderState contCrowdParam");
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
        
        //Max CrowdAgents for the crowd.
        contCrowdParam.addChild(new Label("Max Agents"), "split 2, growx"); 
        doc = new DocumentModelFilter();
        doc.setInputTransform(TextFilters.numeric());
        doc.setText("100");
        fieldMaxAgents = contCrowdParam.addChild(new TextField(doc));
        fieldMaxAgents.setSingleLine(true);
        fieldMaxAgents.setPreferredWidth(50);
        
        //Max CrowdAgent radius for an agent in the crowd.
        contCrowdParam.addChild(new Label("Max Agent Radius"), "split 2, growx");
        fieldMaxAgentRadius = contCrowdParam.addChild(new TextField("0.6"));
        fieldMaxAgentRadius.setSingleLine(true);
        fieldMaxAgentRadius.setPreferredWidth(50);                    
  
        
        
        //Container for list movement.
        Container contListMoveType = new Container(new MigLayout("wrap"));
        contListMoveType.setName("CrowdBuilderState contListMoveType");
        contListMoveType.setAlpha(0, false);
        contCrowd.addChild(contListMoveType, "top");
        
        //Movement types for the crowd.
        contListMoveType.addChild(new Label("Movement Type"));
        //Movement types for crowd
        listMoveType = contListMoveType.addChild(new ListBox<>());
        listMoveType.setName("listMoveType");
        listMoveType.getSelectionModel().setSelection(0);
        listMoveType.getModel().add("BETTER_CHARACTER_CONTROL");
        listMoveType.getModel().add("DIRECT");
        listMoveType.getModel().add("CUSTOM");
        listMoveType.getModel().add("NONE");
        
        
        
        //Container that holds to Active Crowds.
        Container contActiveCrowds = new Container(new MigLayout("wrap"));
        contActiveCrowds.setName("CrowdBuilderState contActiveCrowds");
        contActiveCrowds.setAlpha(0, false);
        contCrowd.addChild(contActiveCrowds, "wrap, flowy, growx, growy");
        
        contActiveCrowds.addChild(new Label("Active Crowds"));
        listActiveCrowds = contActiveCrowds.addChild(new ListBox<>(), "growx, growy"); 
        listActiveCrowds.setName("listActiveCrowds");
        selectionRef = listActiveCrowds.getSelectionModel().createReference();  
        modelRef = listActiveCrowds.getModel().createReference();
        //Button to stop the Crowd.
        contActiveCrowds.addChild(new ActionButton(new CallMethodAction("Shutdown Crowd", this, "shutdown")), "top");
        
        
        
        //Give this its own row so can can align parameters with the listBox
        Container contAvoidLabel = new Container(new MigLayout(null));
        contAvoidLabel.setName("CrowdBuilderState contAvoidLabel");
        contAvoidLabel.setAlpha(0, false);
        contAvoidLabel.addChild(new Label("Obstacle Avoidance Parameters")); 
        contCrowd.addChild(contAvoidLabel, "wrap");
        
        //Container that holds the obstacle avoidance parameters for CrowdAgents.
        Container contAvoidance = new Container(new MigLayout("wrap", "[grow]"));
        contAvoidance.setName("CrowdBuilderState contAvoidance");
        contAvoidance.setAlpha(0, false);
        contCrowd.addChild(contAvoidance, "growx, growy");
                
        //Velocity Bias.
        contAvoidance.addChild(new Label("velBias"), "split 2, growx"); 
        fieldVelocityBias = contAvoidance.addChild(new TextField("0.4"));
        fieldVelocityBias.setSingleLine(true);
        fieldVelocityBias.setPreferredWidth(50);
        
        //Weight desired velocity.
        contAvoidance.addChild(new Label("weightDesVel"), "split 2, growx"); 
        fieldWeightDesVel = contAvoidance.addChild(new TextField("2.0"));
        fieldWeightDesVel.setSingleLine(true);
        fieldWeightDesVel.setPreferredWidth(50);
        
        //Weight current velocity.
        contAvoidance.addChild(new Label("weightCurVel"), "split 2, growx"); 
        fieldWeightCurVel = contAvoidance.addChild(new TextField(".75"));
        fieldWeightCurVel.setSingleLine(true);
        fieldWeightCurVel.setPreferredWidth(50);
        
        //Weight side.
        contAvoidance.addChild(new Label("fieldWeightSide"), "split 2, growx"); 
        fieldWeightSide = contAvoidance.addChild(new TextField(".75"));
        fieldWeightSide.setSingleLine(true);
        fieldWeightSide.setPreferredWidth(50);        
        
        //Weight to impact.
        contAvoidance.addChild(new Label("fieldWeightToi"), "split 2, growx"); 
        fieldWeightToi = contAvoidance.addChild(new TextField("2.5"));
        fieldWeightToi.setSingleLine(true);
        fieldWeightToi.setPreferredWidth(50);  
        
        //Horrizon time.
        contAvoidance.addChild(new Label("fieldHorizTime"), "split 2, growx"); 
        fieldHorizTime = contAvoidance.addChild(new TextField("2.5"));
        fieldHorizTime.setSingleLine(true);
        fieldHorizTime.setPreferredWidth(50);                
                
        //Grid Size.
        contAvoidance.addChild(new Label("fieldGridSize"), "split 2, growx"); 
        doc = new DocumentModelFilter();
        doc.setInputTransform(TextFilters.numeric());
        doc.setText("33");
        fieldGridSize = contAvoidance.addChild(new TextField(doc));
        fieldGridSize.setSingleLine(true);
        fieldGridSize.setPreferredWidth(50);
        
        //Adaptive Divisions.
        contAvoidance.addChild(new Label("adaptiveDivs"), "split 2, growx"); 
        doc = new DocumentModelFilter();
        doc.setInputTransform(TextFilters.numeric());
        doc.setText("7");
        fieldAdaptiveDivs = contAvoidance.addChild(new TextField(doc));
        fieldAdaptiveDivs.setSingleLine(true);
        fieldAdaptiveDivs.setPreferredWidth(50);
        
        //Adaptive Rings.
        contAvoidance.addChild(new Label("adaptiveRings"), "split 2, growx"); 
        doc = new DocumentModelFilter();
        doc.setInputTransform(TextFilters.numeric());
        doc.setText("2");
        fieldAdaptiveRings = contAvoidance.addChild(new TextField(doc));
        fieldAdaptiveRings.setSingleLine(true);
        fieldAdaptiveRings.setPreferredWidth(50);
        
        //Adaptive Depth.
        contAvoidance.addChild(new Label("adaptiveDepth"), "split 2, growx"); 
        doc = new DocumentModelFilter();
        doc.setInputTransform(TextFilters.numeric());
        doc.setText("5");
        fieldAdaptiveDepth = contAvoidance.addChild(new TextField(doc));
        fieldAdaptiveDepth.setSingleLine(true);
        fieldAdaptiveDepth.setPreferredWidth(50);        
        
        
        
        //Holds the listbox for avoidance parameters.
        Container contListBoxAvoidance = new Container(new MigLayout("wrap", "[grow]"));
        contListBoxAvoidance.setName("CrowdBuilderState contListBoxAvoidance");
        contListBoxAvoidance.setAlpha(0, false);
        contCrowd.addChild(contListBoxAvoidance, "wrap, growx, top");
        
        //Parameters list.
        listBoxAvoidance = contListBoxAvoidance.addChild(new ListBox<>(), "growx"); 
        listBoxAvoidance.setName("listBoxAvoidance");
        listBoxAvoidance.setVisibleItems(1);
        
        //The ObstacleAvoidanceParams string for the listbox.      
        for (int i = 0; i < 8; i++) {
            String params = "<=====    " + i + "    =====>\n"
                    + defaultOAP; 
                
            listBoxAvoidance.getModel().add(params);
        }
        listBoxAvoidance.getSelectionModel().setSelection(0);
        
        //Update a parameter button.
        Button butParam = contListBoxAvoidance.addChild(new Button("Update Parameter"));
        
        MouseEventControl.addListenersToSpatial(butParam, new DefaultMouseListener() {
            @Override
            protected void click( MouseButtonEvent event, Spatial target, Spatial capture ) {                
                updateParam();
            }
        });        
        
        
        
        //Holds the Legend and Setup buttons.
        Container contButton = new Container(new MigLayout(null, // Layout Constraints
                "[]push[][]")); // Column constraints [min][pref][max]
        contButton.setName("CrowdBuilderState contButton");
        contButton.setAlpha(1, false);
        contCrowd.addChild(contButton, "growx, span 2"); //cell col row span w h
        
        //Help
        contButton.addChild(new ActionButton(new CallMethodAction("Help", this, "showHelp")));
        //Button to start the Crowd.
        contButton.addChild(new ActionButton(new CallMethodAction("Start Crowd", this, "startCrowd")));
        
        
        
        //Create the container that will hold the tab panel for BuildGridGui and 
        //BuildParamGui gui.
        contTabs = new Container(new MigLayout("wrap"));
        contTabs.setName("CrowdBuilderState contTabs");
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
        
        
        
        //Add a rollup panel so can hide CrowdAgent parameters.
        RollupPanel rollAgentParam = new RollupPanel("Expand / Collapse", 
                getState(AgentParamState.class).getContAgentParams(), "glass");
        rollAgentParam.getTitleContainer().setLayout(new MigLayout(null, "[grow, fill]"));
        rollAgentParam.getTitleElement().removeFromParent();
        rollAgentParam.getTitleContainer().addChild(rollAgentParam.getTitleElement());
        rollAgentParam.getTitleElement().setTextHAlignment(HAlignment.Center);
        rollAgentParam.setAlpha(0, false); 
        rollAgentParam.setOpen(false);
        tabPanel.addTab("Agent Parameters", rollAgentParam);
        
        
        
        int height = getApplication().getCamera().getHeight();
        contTabs.setLocalTranslation(new Vector3f(0, height, 0));
        
        ((SimpleApplication) getApplication()).getGuiNode().attachChild(contTabs);
    }

    /**
     * Called by the DemoApplication F1 button ActionListener as part of a chain 
     * detachment of states. This is the start of the detachment chain.
     * CrowdBuilderState(onDisable)=>AgentParamState(onDisable)=>AgentGridState(onDisable)
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

        //Have to check for both selectionRef(the selections themselves) and 
        //modelRef(the list of crowd names) updates to fully know whether the 
        //reference has been updated.
        if( selectionRef.update() || modelRef.update()) {
            // Selection has changed and the model or selection is empty.
            if ( selectionRef.get().isEmpty() ||  modelRef.get().isEmpty()) {
                //Load defaults since there is no data to update.
                LOG.info("Update Loop empty reference - selectionRef [{}] modelRef [{}]", selectionRef.get().isEmpty(), modelRef.get().isEmpty());
                //The ObstacleAvoidanceParams string for the listbox.      
                for (int i = 0; i < 8; i++) {
                    String params = "<=====    " + i + "    =====>\n" + defaultOAP; 
                    //Remove selected parameter from listBoxAvoidance. When 
                    //remove or insert is used on a Lemur ListBox, getSelection()
                    //will not be updated to anything other than the last item 
                    //in the ListBox. If the ListBox size is not affected
                    //by removal, such as following it with an insert, this can 
                    //be ignored. Otherwise it's best to setSelection(-1) and
                    //force the user to make a selection.
                    listBoxAvoidance.getModel().remove(i);
                    //Insert the new parameters into listBoxAvoidance.
                    listBoxAvoidance.getModel().add(i, params);
                }
            } else {
                Crowd crowd = getSelectedCrowd();
                //Look for the crowd in mapCrowds rather than pulling 
                //directly from CrowdManager in case CrowdState is running
                //at same time as gui.
                if (crowd != null) {
                    for (int i = 0; i < 8; i++) {
                        ObstacleAvoidanceParams oap = crowd.getObstacleAvoidanceParams(i);
                        //Remove selected parameter.
                        listBoxAvoidance.getModel().remove(i);
                        //Insert the new parameters into the list by converting
                        //the oap to string.
                        listBoxAvoidance.getModel().add(i, oapToString(oap, i));
                    }
                } 
            }
        }

    }
    
    //Explains the Crowd parameters.
    private void showHelp() {
                
        String[] msg = { 
        "NavMesh - The navigation mesh to use for planning.",
        " ",
        "Crowd Name - The name for this crowd. Each crowd must have a unique name. Spaces",
        "count in the naming so if there is added space after a crowd name, that crowd will", 
        "be considered unique.",  
        " ",
        "Max Agents - The maximum number of agents the crowd can manage. [Limit: >= 1]",
        "  ",
        "Max Agent Radius - The maximum radius of any agent that will be added to the crowd.",
        " ",
        "Movement Type - Each type determines how movement is applied to an agent.",
        " ",
        "* BETTER_CHARACTER_CONTROL - As the name implies, uses physics and the ",
        "BetterCharacterControl to set move and view direction. ",
        " ",
        "* DIRECT - Direct setting of the agents translation. No controls are needed for,",
        "movement but you must set the view direction yourself.",
        " ",
        "* CUSTOM - With custom, you implement the applyMovement() method from the",
        "ApplyFunction interface. This will give you access to the agents CrowdAgent object, the",
        "new position and the velocity of the agent.",
        " ",
        "* NONE - No movement is implemented. You can freely interact with the crowd and",
        "implement your own movement soloutions.",
        " ",
        "Active Crowds - The list of active crowds. A crowd must be selected before any agents", 
        "can be added to a crowd or targets for agents can be set.", 
        " ",
        "Obstacle Avoidance Parameters - The shared avoidance configuration for an Agent", 
        "inside the crowd. When first instantiating the crowd, you are allotted eight parameter", 
        "objects in total. All eight slots are filled with the defaults listed below.", 
        " ",
        "[Defaults]",
        "velBias             = 0.4f \t    horizTime         = 2.5f",
        "weightDesVel = 2.0f \t    gridSize             = 33",
        "weightCurVel  = 0.75f \t adaptiveDivs     = 7",
        "weightSide      = 0.75f \t adaptiveRings   = 2",
        "weightToi         = 2.5f \t   adaptiveDepth  = 5",
        " ",
        "You may not remove any default parameter from the Crowd, however, you can",
        "overwrite them. To change any parameter, select the crowd to be updated from the",
        "Active Crowds window, set the parameters you desire in the Obstacle Avoidance", 
        "Parameters section, then select any parameter from the list and click the",
        "[ Update Parameter ] button.",
        " ",
        "* velBias - The velocity bias describes how the sampling patterns is offset from the (0,0)", 
        "based on the desired velocity. This allows to tighten the sampling area and cull a lot of", 
        "samples. [Limit: 0-1]",
        " ",
        "* weightDesVel - How much deviation from desired velocity is penalized, the more", 
        "penalty applied to this, the more \"goal oriented\" the avoidance is, at the cost of getting", 
        "more easily stuck at local minima. [Limit: >= 0]",
        " ",
        "* weightCurVel - How much deviation from current velocity is penalized, the more", 
        "penalty applied to this, the more stubborn the agent is. This basically is a low pass filter,", 
        "and very important part of making things work.",
        " ",
        "* weightSide - In order to avoid reciprocal dance, the agents prefer to pass from right,", 
        "this weight applies penalty to velocities which try to take over from the wrong side.",
        " ",
        "* weightToi - How much penalty is added based on time to impact. Too much penalty", 
        "and the agents are shy, too little and they avoid too late.",
        " ",
        "* horizTime - Time horizon, this affects how early the agents start to avoid each other.", 
        "Too long horizon and the agents are scared of going through tight spots, and too small,", 
        "and they avoid too late (closely related to weightToi).",
        " ",
        "* gridSize - ???",
        " ",
        "* adaptiveDivs - Number of divisions per ring. [Limit: 1-32]",
        " ",
        "* adaptiveRings - Number of rings. [Limit: 1-4]",
        " ",
        "* adaptiveDepth - Number of iterations at best velocity." 
        };
        
        Container window = new Container(new MigLayout("wrap"));
        ListBox<String> listScroll = window.addChild(new ListBox<>());
        listScroll.getModel().addAll(Arrays.asList(msg));
        listScroll.setPreferredSize(new Vector3f(500, 400, 0));
        listScroll.setVisibleItems(20);
        window.addChild(new ActionButton(new CallMethodAction("Close", window, "removeFromParent")), "align 50%");
        getState(GuiUtilState.class).centerComp(window);
        //This assures clicking outside of the message should close the window 
        //but not activate underlying UI components.
        GuiGlobals.getInstance().getPopupState().showPopup(window, PopupState.ClickMode.ConsumeAndClose, null, null);
    }
    
    /**
     * Shuts down crowds listed in the Active Crowds list. When CrowdState is
     * enabled this will not be affected due to using the value of mapCrowds
     * to remove the Crowd from the CrowdManager. We skip the getters getCrowdName,
     * getSelectedCrowd because getSelectedCrowd calls getCrowdName internally
     * also so taking the long way home is more efficient.
     */
    private void shutdown() {

        //Get the Crowd from selectedParam. we need this for accurate removal 
        //from listActiveCrowds.
        Integer selectedCrowd = listActiveCrowds.getSelectionModel().getSelection();
        
        //Check to make sure the crowd has been selected.
        if (selectedCrowd == null) {
            displayMessage("Select a crowd from the [ Active Crowds ] list.", 0);
            return;
        }
        
        //Get the crowds name from the listActiveCrowds selectedParam.
        String crowdName = listActiveCrowds.getModel().get(selectedCrowd);

        //We check mapCrowds to see if the key exists. If not, go no further.
        if (!mapCrowds.containsKey(crowdName)) {
            displayMessage("No crowd found by that name.", 0);
            return;
        }
        
        //We have a valid crowdName so remove it from the map, the CrowdManager 
        //and the listActiveCrowds listbox.
        Iterator<Map.Entry<String, CrowdNQuery>> iterator = mapCrowds.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CrowdNQuery> entry = iterator.next();
            if (entry.getKey().equals(crowdName)) {
                //To fully remove the crowdName we have to remove it from the 
                //CrowdManager, mapCrowds (removes the query object also), and 
                //the listActiveCrowds.
                getState(CrowdManagerAppstate.class).getCrowdManager().removeCrowd(entry.getValue().getCrowd());
                listActiveCrowds.getModel().remove((int)selectedCrowd);
                //Lemur getSelected() does not update if you remove or insert 
                //into a listBox. Best to set it to -1 (unselected) and force
                //user to reselect.
                listActiveCrowds.getSelectionModel().setSelection(-1);
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
            displayMessage("You must enter a [ NavMesh ] name.", 0);
            return;
        } else {
            mesh = fieldNavMesh.getText();
        }
        
        
        //The name of this crowd.                
        if (fieldCrowdName.getText().isEmpty()) {
            displayMessage("You must enter a [ Crowd ] name.", 0);
            return;
        } else if (mapCrowds.containsKey(fieldCrowdName.getText())) {
            displayMessage(
                      "[ " + fieldCrowdName.getText() 
                    + " ] has already been activated. "
                    + "Change the crowd name or remove the existing crowd before proceeding.", 0);
            return;
        } else {
            crowdName = fieldCrowdName.getText();
        }
        
        //The max CrowdAgents for the crowd. Uses numeric doc filter to prevent bad data.
        if (fieldMaxAgents.getText().isEmpty()) {
            displayMessage("[ Max Agents ] requires a valid int value.", 0);
            return;
        } else {
            maxAgents = new Integer(fieldMaxAgents.getText());
            //Stop useless input.
            if (maxAgents < 1) {
                displayMessage("[ Max Agents ] requires a int value >= 1", 0);
                return;
            }
        }
        
        //The max CrowdAgent radius for an agent in the crowd.
        if (!getState(GuiUtilState.class).isNumeric(fieldMaxAgentRadius.getText()) 
        ||  fieldMaxAgentRadius.getText().isEmpty()) {
            displayMessage("[ Max Agent Radius ] requires a valid float value.", 0);
            return;
        } else {
            maxAgentRadius = new Float(fieldMaxAgentRadius.getText());
            //Stop negative input.
            if (maxAgentRadius <= 0.0f ) {
                displayMessage("[ Max Agent Radius ] requires a float value between > 0.", 0);
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
            
            //Create the crowd.
            Crowd crowd = new Crowd(applicationType, maxAgents, maxAgentRadius, navMesh);
            
            //Add to CrowdManager, mapCrowds, and listActiveCrowds.
            getState(CrowdManagerAppstate.class).getCrowdManager().addCrowd(crowd);
            mapCrowds.put(crowdName, new CrowdNQuery(crowd, query));
            listActiveCrowds.getModel().add(crowdName); 
        } catch (IOException | NoSuchFieldException | IllegalAccessException ex) {
            LOG.error("{} {}", CrowdBuilderState.class.getName(), ex);
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
    private void updateParam() {
        float velBias;
        float weightDesVel;
        float weightCurVel;
        float weightSide;
        float weightToi;
        float horizTime;
        int gridSize;
        int adaptiveDivs;
        int adaptiveDepth;
        int adaptiveRings;
        Integer selectedParam;
                
        //The velocity bias settings.
        if (!getState(GuiUtilState.class).isNumeric(fieldVelocityBias.getText()) 
        ||  fieldVelocityBias.getText().isEmpty()) {
            displayMessage("[ velocityBias ] requires a valid float value.", 0);
            return;
        } else {
            velBias = new Float(fieldVelocityBias.getText());
            //Stop negative input.
            if (velBias < 0.0f || velBias > 1) {
                displayMessage("[ fieldVelocityBias ] requires a float value between 0 and 1 inclusive.", 0);
                return;
            }
        }
        
        //The weighted desired velocity settings.
        if (!getState(GuiUtilState.class).isNumeric(fieldWeightDesVel.getText()) 
        ||  fieldWeightDesVel.getText().isEmpty()) {
            displayMessage("[ weightDesVel ] requires a valid float value.", 0);
            return;
        } else {
            weightDesVel = new Float(fieldWeightDesVel.getText());
            //Stop negative input.
            if (weightDesVel < 0.0f) {
                displayMessage("[ weightDesVel ] requires a float value >= 0.", 0);
                return;
            }
        }

        //The weighted current velocity settings.
        if (!getState(GuiUtilState.class).isNumeric(fieldWeightCurVel.getText()) 
        ||  fieldWeightCurVel.getText().isEmpty()) {
            displayMessage("[ fieldWeightCurVel ] requires a valid float value.", 0);
            return;
        } else {
            weightCurVel = new Float(fieldWeightCurVel.getText());
            //Stop negative input.
            if (weightCurVel < 0.0f) {
                displayMessage("[ fieldWeightCurVel ] requires a float value >= 0.", 0);
                return;
            }
        }
        
        //The weighted side settings.
        if (!getState(GuiUtilState.class).isNumeric(fieldWeightSide.getText()) 
        ||  fieldWeightSide.getText().isEmpty()) {
            displayMessage("[ fieldWeightSide ] requires a valid float value.", 0);
            return;
        } else {
            weightSide = new Float(fieldWeightSide.getText());
            //Stop negative input.
            if (weightSide < 0.0f) {
                displayMessage("[ fieldWeightSide ] requires a float value >= 0.", 0);
                return;
            }
        }
        
        //The weight to impact settings.
        if (!getState(GuiUtilState.class).isNumeric(fieldWeightToi.getText()) 
        ||  fieldWeightToi.getText().isEmpty()) {
            displayMessage("[ fieldWeightToi ] requires a valid float value.", 0);
            return;
        } else {
            weightToi = new Float(fieldWeightToi.getText());
            //Stop negative input.
            if (weightToi < 0.0f) {
                displayMessage("[ fieldWeightToi ] requires a float value >= 0.", 0);
                return;
            }
        }        
                
        //The horizon settings.
        if (!getState(GuiUtilState.class).isNumeric(fieldHorizTime.getText()) 
        ||  fieldHorizTime.getText().isEmpty()) {
            displayMessage("[ fieldHorizTime ] requires a valid float value.", 0);
            return;
        } else {
            horizTime = new Float(fieldHorizTime.getText());
            //Stop negative input.
            if (weightToi < 0.0f) {
                displayMessage("[ fieldHorizTime ] requires a float value >= 0.", 0);
                return;
            }
        }       
        
        //The grid size settings. Uses numeric doc filter to prevent bad data.
        if (fieldGridSize.getText().isEmpty()) {
            displayMessage("[ fieldGridSize ] requires a valid int value.", 0);
            return;
        } else {
            gridSize = new Integer(fieldGridSize.getText());
            //Stop useless input.
            if (gridSize < 1) {
                displayMessage("[ fieldGridSize ] requires a int value >= 1.", 0);
                return;
            }
        }
        
        //The adaptive divisions settings. Uses numeric doc filter to prevent
        //bad data.
        if (fieldAdaptiveDivs.getText().isEmpty()) {
            displayMessage("[ adaptiveDivs ] requires a valid int value.", 0);
            return;
        } else {
            adaptiveDivs = new Integer(fieldAdaptiveDivs.getText());
            //Stop useless input.
            if (adaptiveDivs < 1 || adaptiveDivs > 32) {
                displayMessage("[ adaptiveDivs ] requires a int value between 1 and 32 inclusive.", 0);
                return;
            }
        }
        
         //The adaptive depth settings. Uses numeric doc filter to prevent bad data.
        if (fieldAdaptiveDepth.getText().isEmpty()) {
            displayMessage("[ adaptiveDepth ] requires a valid int value.", 0);
            return;
        } else {
            adaptiveDepth = new Integer(fieldAdaptiveDepth.getText());
            //Stop useless input.
            if (adaptiveDepth < 1 ) {
                displayMessage("[ adaptiveDepth ] requires a int value >= 1.", 0);
                return;
            }
        }
        
        //The adaptive depth settings. Uses numeric doc filter to prevent bad data.
        if (fieldAdaptiveRings.getText().isEmpty()) {
            displayMessage("[ adaptiveRings ] requires a valid int value.", 0);
            return;
        } else {
            adaptiveRings = new Integer(fieldAdaptiveRings.getText());
            //Stop negative input.
            if (adaptiveRings < 1 || adaptiveRings > 4) {
                displayMessage("[ adaptiveRings ] requires a int value between 1 and 4 inclusive.", 0);
                return;
            }
        }
        
        //Get the selectedParam from listBoxAvoidance.
        selectedParam = listBoxAvoidance.getSelectionModel().getSelection();
        
        //Check to make sure a an avoidance parmeter has been selected.
        if (selectedParam == null) {
            displayMessage("You must select a [ Parameter ] from the list before it can be updated.", 0);
            return;
        }

        ObstacleAvoidanceParams params  = new ObstacleAvoidanceParams();
        params.velBias          = velBias;
        params.weightDesVel     = weightDesVel;
        params.weightCurVel     = weightCurVel;
        params.weightSide       = weightSide;
        params.weightToi        = weightToi;
        params.horizTime        = horizTime;
        params.gridSize         = gridSize;
        params.adaptiveDivs     = adaptiveDivs;
        params.adaptiveDepth    = adaptiveDepth;
        params.adaptiveRings    = adaptiveRings;

        //Inject the new parameter into the crowd. Check for the crowd in 
        //mapcrowds and if exists, update OAP params. Pulls the crowd reference 
        //from the CrowdNQuery obj rather than the CrowdManager in case 
        //CrowdState is running. This will keep our crowd lookups in sync.
        if (getSelectedCrowd() != null) {
            getSelectedCrowd().setObstacleAvoidanceParams(selectedParam, params);
            //Remove selected parameter from listBoxAvoidance.
            listBoxAvoidance.getModel().remove((int)selectedParam);
            //Insert the new parameters into listBoxAvoidance.
            listBoxAvoidance.getModel().add(selectedParam, oapToString(params, selectedParam));
        } else {
            LOG.error("Failed to find the selected crowd in mapCrowds [{}]", getCrowdName());
            displayMessage("You must select a [ Active Crowd ] " 
                    + "from the list before a parameter can be updated.", 0);
        }
    }
    

    
    //Logs crowd info to the console.
    protected void dumpActiveAgents(int i) {
        Crowd crowd = getState(CrowdManagerAppstate.class).getCrowdManager().getCrowd(i);
        
        for (CrowdAgent ag : crowd.getActiveAgents()) {
            if (ag.isActive()) {
                LOG.info("Crowd         [{}] Active Agents [{}]", i, crowd.getActiveAgents().size());
                LOG.info("State         [{}]", ag.state);
                LOG.info("Pos           [{},{},{}]",ag.npos[0], ag.npos[1], ag.npos[2]);
                LOG.info("Target Pos    [{},{},{}]",ag.targetPos[0], ag.targetPos[1], ag.targetPos[2]);
                LOG.info("Target Ref    [{}]",ag.targetRef);
            }
        }
    }
    
    /**
     * Formats a Obstacle Avoidance Parameter into string form for insertion into  
     * a listBoxAvoidance selection.
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
     * 
     * @param oap The Obstacle Avoidance Parameter that needs string formating.
     * @param idx The index of the listboxAvoidance parameter.
     * @return The formated string.
     */
    private String oapToString(ObstacleAvoidanceParams oap, int idx) {
        //If run into threading problems use StringBuffer.
        StringBuilder buf               = new StringBuilder();
        String i                        = null;
        String str = 
          "<=====    " + idx + "    =====>\n"  
        + "velBias              = $b\n"
        + "weightDesVel  = $v\n"
        + "weightCurVel   = $V\n"
        + "weightSide       = $s\n"
        + "weightToi         = $i\n"
        + "horizTime         = $h\n"
        + "gridSize            = $g\n"
        + "adaptiveDivs    = $d\n"               
        + "adaptiveRings  = $r\n"
        + "adaptiveDepth = $D";
        LOG.info("<========== BEGIN CrowdBuilderState oapToString [{}]==========>", idx);
        for ( int j = 0; j < str.length(); j++ ) {
            if ( str.charAt(j) != '$' ) {
                buf.append(str.charAt(j));
                continue;
            }
            
            char charAt = str.charAt(++j);
            
            switch (charAt) {
                case 'b': //Velocity Bias
                    i = "" + oap.velBias;
                    LOG.info("velBias       [{}]", oap.velBias);
                    break;
                case 'd': //Adaptive Divisions
                    i = "" + oap.adaptiveDivs;
                    LOG.info("adaptiveDivs  [{}]", oap.adaptiveDivs);
                    break;
                case 'D': //Adaptive Depth
                    i = "" + oap.adaptiveDepth;
                    LOG.info("adaptiveDepth [{}]", oap.adaptiveDepth);
                    break;
                case 'g': //Grid Size
                    i = "" + oap.gridSize;
                    LOG.info("gridSize      [{}]", oap.gridSize);
                    break;
                case 'h': //Horizon Time
                    i = "" + oap.horizTime;
                    LOG.info("horizTime     [{}]", oap.horizTime);
                    break;
                case 'i': //Weight To Impact
                    i = "" + oap.weightToi;
                    LOG.info("weightToi     [{}]", oap.weightToi);
                    break;
                case 'r': //Adaptive Rings
                    i = "" + oap.adaptiveRings;
                    LOG.info("adaptiveRings [{}]", oap.adaptiveRings);
                    break;
                case 's': //Weight Side
                    i = "" + oap.weightSide;
                    LOG.info("weightSide    [{}]", oap.weightSide);
                    break;
                case 'v': //Weight Desired Velocity
                    i = "" + oap.weightDesVel;
                    LOG.info("weightDesVel  [{}]", oap.weightDesVel);
                    break;
                case 'V': //Weight Current Velocity
                    i = "" + oap.weightCurVel;
                    LOG.info("weightCurVel  [{}]", oap.weightCurVel);
                    break;
            }
            buf.append(i);
        } 
        LOG.info("<========== END CrowdBuilderState oapToString [{}] ==========>", idx);
        return buf.toString();
        
    }
    
    /**
     * Displays a modal popup message.
     * 
     * @param txt The text for the popup.
     * @param width The maximum width for wrap. 
     */
    private void displayMessage(String txt, float width) {
        GuiGlobals.getInstance().getPopupState()
                    .showModalPopup(getState(GuiUtilState.class)
                            .buildPopup(txt, width));
    }
    
    /**
     * Gets the query object for any selected crowd.
     * 
     * @return The query object for a selected crowd or null if the 
     * crowd has not been selected in the Active Crowds list or if the query 
     * object doesn't exist in the mapCrowds list.
     */
    public NavMeshQuery getQuery() {
        
        String crowdName = getCrowdName();
        
        //Check to make sure a crowdName has been selected.
        if (crowdName == null || !mapCrowds.containsKey(crowdName)) {
            return null;
        } 
        
        return mapCrowds.get(crowdName).getQuery();
    }    
    
    /**
     * Gets the currently selected crowdName from the Active Crowds list as a 
     * crowd object.
     * 
     * @return The string name of the selection.
     */
    public Crowd getSelectedCrowd() {
        String crowdName = getCrowdName();
        
        if (crowdName == null || !mapCrowds.containsKey(crowdName)) {
            return null;
        }
        
        return mapCrowds.get(crowdName).getCrowd();
    }    
    
    /**
     * Takes a selection from the Active Crowds list and converts it to string.
     * 
     * @return The selection as a string or null. 
     */
    public String getCrowdName() {
        //Get the crowdName from listActiveCrowds.
        Integer selectedCrowd = listActiveCrowds.getSelectionModel().getSelection();

        //Check to make sure a crowd has been selected.
        if (selectedCrowd == null) {
            return null;
        }
        
        //Convert seltion to string.
        return listActiveCrowds.getModel().get(selectedCrowd);
    }
    
    /**
     * Gets a crowd number from the CrowdManager by checking the CrowdManager 
     * for the crowd object. Not really useful for anything other than logging.
     * 
     * @param crowd The crowd to lookup.
     * @return The crowd number for the given crowd.
     */
    public int getCrowdNumber(Crowd crowd) {
        int numberOfCrowds = getState(CrowdManagerAppstate.class).getCrowdManager().getNumberOfCrowds();
        for (int i = 0; i < numberOfCrowds; i++) {
            if (getState(CrowdManagerAppstate.class).getCrowdManager().getCrowd(i).equals(crowd)) {
                return i;
            }
        }
        return -1;
    }
    
    private class CrowdNQuery {
        private Crowd crowd;
        private NavMeshQuery query;

        public CrowdNQuery(Crowd crowd, NavMeshQuery query) {
            this.crowd = crowd;
            this.query = query;
        }

        /**
         * @return the crowd
         */
        public Crowd getCrowd() {
            return crowd;
        }

        /**
         * @return the query
         */
        public NavMeshQuery getQuery() {
            return query;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 47 * hash + Objects.hashCode(this.crowd);
            hash = 47 * hash + Objects.hashCode(this.query);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final CrowdNQuery other = (CrowdNQuery) obj;
            if (!Objects.equals(this.crowd, other.crowd)) {
                return false;
            }
            if (!Objects.equals(this.query, other.query)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "CrowdQueryObj{" + "crowd=" + crowd + ", query=" + query + '}';
        }
        
    }
}
