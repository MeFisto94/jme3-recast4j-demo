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
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.recast4j.demo.controls.CrowdAgentControl;
import com.jme3.recast4j.demo.controls.NavMeshChaserControl;
import com.jme3.recast4j.demo.layout.MigLayout;
import com.jme3.scene.Node;
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
import com.simsilica.lemur.component.TbtQuadBackgroundComponent;
import com.simsilica.lemur.event.DefaultMouseListener;
import com.simsilica.lemur.event.MouseEventControl;
import com.simsilica.lemur.style.Attributes;
import com.simsilica.lemur.style.BaseStyles;
import com.simsilica.lemur.style.Styles;
import com.simsilica.lemur.text.DocumentModelFilter;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.recast4j.detour.crowd.CrowdAgentParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Robert
 */
public class AgentGenState extends BaseAppState {

    private static final Logger LOG = LoggerFactory.getLogger(AgentGenState.class.getName());

    private TextField fieldDistance;
    private DocumentModelFilter doc;
    private ListBox listBoxAvoidance;
    private ListBox listBoxAgent;
    private ListBox listBoxSize;
    private ListBox listBoxTests;
    private TextField fieldPosX;
    private TextField fieldPosY;
    private TextField fieldPosZ;
    private TextField fieldTestName;
    private Checkbox checkTurns;
    private Checkbox checkAvoid;
    private Checkbox checkTopo;
    private Checkbox checkVis;
    private Checkbox checkSep;
    private Map<String, Test> mapTests;
    private boolean newTest;
    private boolean checkTests;
    private Container contTabs;
    private Container contAgentGen;
    
    @SuppressWarnings("unchecked")
    @Override
    protected void initialize(Application app) {
        // Holds all active tests.
        mapTests = new HashMap<>();
        
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
        
        attrs = styles.getSelector("glass");
        attrs.set("fontSize", 12);
        
        //Container for crowd agent configuration.
        //Set the column minimum to grow so all children can grow to the edge.
        //Set to fill so the components in that column will default to a "growx"
        //constraint. Note that this property does not affect the size for the row,
        //but rather the sizes of the components in the row.
        contAgentGen = new Container(new MigLayout(
                "wrap 3", // Layout Constraints
                "[grow, fill][][]")); // Column constraints [min][pref][max]
        contAgentGen.setName("TestGenState crowdCont");
        contAgentGen.setAlpha(0, false);
        
        // First row.
        //Set column min to grow or minimum will prevent fieldTestName from 
        //growing fully to edge.
        Container contRow1 = new Container(new MigLayout(
                "wrap 3", // Layout Constraints
                "[grow][][]")); // Column constraints [min][pref][max]
        contRow1.setName("TestGenState rowOneCont");
        //contRow1 wraps at 3 componets based on the Crowd Separation row.
        //Span all 3 cells for the Test Name row so can place fieldTestName in 
        //same cell and growx and stretch to the edge of contRow1.
        contRow1.addChild(new Label("Test Name"), "span 3");
        
        doc = new DocumentModelFilter();
        //placeholder for filtering
        //Placed in Label("Test Name") cell so will align nicely to the end of
        //of the label. growx direction to stretch the TextField to the edge of
        //contRow1.
        fieldTestName = contRow1.addChild(new TextField(doc), "cell 0 0, growx");
        fieldTestName.setSingleLine(true);
        
        contRow1.addChild(new Label("Crowd Separation"));
        doc = new DocumentModelFilter();
        //placeholder for filtering        
        fieldDistance = contRow1.addChild(new TextField(doc));
        fieldDistance.setSingleLine(true);
        fieldDistance.setPreferredWidth(50);
        fieldDistance.setText("1.0");
        contRow1.addChild(new Label(".float"));
        //contRow1 uses 1 of 3 cells, span to fill row.
        contAgentGen.addChild(contRow1, "span");
           
        //Second row
        //Add model name here. Add the asset path butSetup #Model Section
        Container cont1Row2 = new Container(new MigLayout("wrap"));
        cont1Row2.setName("TestGenState rowTwoCont1");
        cont1Row2.addChild(new Label("Agent"));
        listBoxAgent = cont1Row2.addChild(new ListBox(), "align 50%");
        listBoxAgent.getModel().add("Jamie");
        listBoxAgent.getSelectionModel().setSelection(0);
        contAgentGen.addChild(cont1Row2);
        
        Container cont2Row2 = new Container(new MigLayout("wrap"));
        cont2Row2.setName("TestGenState rowTwoCont2");
        cont2Row2.addChild(new Label("Crowd Size"));
        listBoxSize = cont2Row2.addChild(new ListBox(), "align 50%");
        int size = 1;
        for (int i = 0; i < 15; i++) {
            listBoxSize.getModel().add(size);
            size++;
        }
        listBoxSize.getSelectionModel().setSelection(0);
        contAgentGen.addChild(cont2Row2);
        
        Container cont3Row2 = new Container(new MigLayout("wrap"));
        cont3Row2.setName("TestGenState rowTwoCont3");
        cont3Row2.addChild(new Label("Avoidance"));
        listBoxAvoidance = cont3Row2.addChild(new ListBox(), "align 50%");
        
        //Have to set this here since Crowd has package-private access the to 
        //the DT_CROWD_MAX_OBSTAVOIDANCE_PARAMS variable. Currently max is 8.
        for (int i = 0; i < 8; i++) {
            listBoxAvoidance.getModel().add(i);
        }
        listBoxAvoidance.getSelectionModel().setSelection(0);
        contAgentGen.addChild(cont3Row2);
        
        //Third row  
        //TODO add a multi selection mode and move this to a ListBox. Change the
        //butSetup mega if/else check when done.
        Container contRow3 = new Container(new MigLayout("wrap"));
        contRow3.setName("TestGenState rowThreeCont");
        contRow3.addChild(new Label("Update Flags"), "span");
        checkTurns = contRow3.addChild(new Checkbox("ANTICIPATE_TURNS"));
        checkAvoid = contRow3.addChild(new Checkbox("OBSTACLE_AVOIDANCE"));
        checkTopo = contRow3.addChild(new Checkbox("OPTIMIZE_TOPO"));
        checkVis = contRow3.addChild(new Checkbox("OPTIMIZE_VIS"));
        checkSep = contRow3.addChild(new Checkbox("SEPARATION"));
        contAgentGen.addChild(contRow3, "span");
        
        //Fourth row
        Container contRow4 = new Container(new MigLayout("wrap 6"));
        contRow4.setName("TestGenState rowFourCont");
        contRow4.addChild(new Label("Start Position"), "span"); 
        //X
        contRow4.addChild(new Label("X:"));
        doc = new DocumentModelFilter();
        //placeholder for filtering
        fieldPosX = contRow4.addChild(new TextField(doc));
        fieldPosX.setSingleLine(true);
        fieldPosX.setPreferredWidth(50);
        fieldPosX.setText("0.0");
        
        //Y
        contRow4.addChild(new Label("Y:"));
        doc = new DocumentModelFilter();
        //placeholder for filtering
        fieldPosY = contRow4.addChild(new TextField(doc));
        fieldPosY.setSingleLine(true);
        fieldPosY.setPreferredWidth(50);
        fieldPosY.setText("0.0");
        
        //Z
        contRow4.addChild(new Label("Z:"));
        doc = new DocumentModelFilter();
        //placeholder for filtering        
        fieldPosZ = contRow4.addChild(new TextField(doc));
        fieldPosZ.setSingleLine(true);
        fieldPosZ.setPreferredWidth(50);
        fieldPosZ.setText("0.0");
        contAgentGen.addChild(contRow4, "span");
        
        //Fifth row. Holds the Active test window and Remove test button.
        Container contRow5 = new Container(new MigLayout("wrap", "[grow]"));
        contRow5.setName("TestGenState rowFiveCont");
        contRow5.addChild(new Label("Active Tests"));
        listBoxTests = contRow5.addChild(new ListBox(), "growx");
        //We use the method call to removeTest to set a boolean of
        contRow5.addChild(new ActionButton(new CallMethodAction("Remove Test", this, "removeTest")));
        contAgentGen.addChild(contRow5, "span");
        
        //Sixth row. Holds the Genrate Agents and Legend buttons.
        //When using the contAgentGen with grow, fill column constraints you have to 
        //set the column constrains for the children containers in the layout if 
        //you want to align components. I am not sure if setting other Lemur parameters
        //would remedy this or if it is expected MigLayout behavior. The MigLayout
        //docs only mention if two components share the same cell that this is
        //the expected behavior, which in this instance, clearly is not the case.
        Container contRow6 = new Container(new MigLayout(
                "wrap 2", // Layout Constraints
                "[]push[][]")); // Column constraints [min][pref][max]
        contRow6.setName("TestGenState rowSixCont");
        //Buttons.
        contRow6.addChild(new ActionButton(new CallMethodAction("Legend", this, "showLegend")));
        Button butSetup = contRow6.addChild(new Button("Generate Agents"));
        MouseEventControl.addListenersToSpatial(butSetup, new DefaultMouseListener() {
            private String testName;
            private float distance = -1;
            private String agent;
            private int flags;
            private Vector3f startPos = new Vector3f(0, 0, 0);
            
            @Override
            protected void click( MouseButtonEvent event, Spatial target, Spatial capture ) {
                
                //Garbage in?, no test.
                if (!isNumeric(fieldDistance.getText()) 
                ||  fieldDistance.getText().isEmpty()) {
                    GuiGlobals.getInstance().getPopupState()
                            .showModalPopup(getState(GuiUtilState.class)
                                    .buildPopup("Crowd Separation requires a valid float value.", 0));
                    return;
                } else {
                    distance = new Float(fieldDistance.getText());
                    //Stop negative distance input for grid.
                    if (distance < 0.1f) {
                        GuiGlobals.getInstance().getPopupState()
                                .showModalPopup(getState(GuiUtilState.class)
                                        .buildPopup("Crowd Separation requires a float value >= 0.1.", 0));
                        return;
                    }
                }
                
                //###### Model Section #####
                //Model to use for the test. Selection is set to 0 when creating 
                //the listBoxAgent so shouldn't need to check for null.
                switch(listBoxAgent.getSelectionModel().getSelection()) {
                    case 0: 
                        agent = "Models/Jaime/Jaime.j3o";
                        break;
                    default: agent = null;
                }
                
                //Number of agents to place into the test in rows and columns.
                //Selection is set to 0 when creating the listBoxSize so 
                //shouldn't need to check for null.
                int size = listBoxSize.getSelectionModel().getSelection() + 1;
                
                //Set the CrowdAgentParams. Ugly but, lemurs ListBox doesn't 
                //have a muli selection model.
                if (!checkTurns.isChecked() 
                &&  !checkAvoid.isChecked() 
                &&  !checkTopo.isChecked() 
                &&  !checkVis.isChecked() 
                &&  !checkSep.isChecked()) {
                    GuiGlobals.getInstance().getPopupState()
                            .showModalPopup(getState(GuiUtilState.class)
                                    .buildPopup("Select at least one Update Flag.", 0));
                    return;
                } else {
                    flags = 0;
                    if (checkTurns.isChecked()) {
                        flags += CrowdAgentParams.DT_CROWD_ANTICIPATE_TURNS;
                    }
                    
                    if (checkAvoid.isChecked()) {
                        flags += CrowdAgentParams.DT_CROWD_OBSTACLE_AVOIDANCE;
                    }
                    
                    if (checkTopo.isChecked()) {
                        flags += CrowdAgentParams.DT_CROWD_OPTIMIZE_TOPO;
                    }
                    
                    if (checkVis.isChecked()) {
                        flags += CrowdAgentParams.DT_CROWD_OPTIMIZE_VIS;
                    }
                    
                    if (checkSep.isChecked()) {
                        flags += CrowdAgentParams.DT_CROWD_SEPARATION;
                    }
                }
                
                //Obstacle Avoidance Type. Selection is set to 0 when creating 
                //the listBoxAvoidance so shouldn't need to check for null. 
                int obstacleAvoidanceType = listBoxAvoidance.getSelectionModel().getSelection();
                
                //The starting position of the test. Sanity check.
                if (!isNumeric(fieldPosX.getText()) || fieldPosX.getText().isEmpty() 
                ||  !isNumeric(fieldPosY.getText()) || fieldPosY.getText().isEmpty() 
                ||  !isNumeric(fieldPosZ.getText()) || fieldPosZ.getText().isEmpty()) {
                    GuiGlobals.getInstance().getPopupState()
                            .showModalPopup(getState(GuiUtilState.class)
                                    .buildPopup("Start Position requires a valid float value.", 0));
                } else {
                    Float x = new Float(fieldPosX.getText());
                    Float y = new Float(fieldPosY.getText());
                    Float z = new Float(fieldPosZ.getText());
                    startPos = new Vector3f(x, y, z);
                }
                
                //The name of this test.                
                if (fieldTestName.getText().isEmpty()) {
                    GuiGlobals.getInstance().getPopupState()
                            .showModalPopup(getState(GuiUtilState.class)
                                    .buildPopup("You must enter a test name.", 0));
                    return;
                } else if (mapTests.containsKey(fieldTestName.getText())) {
                    GuiGlobals.getInstance().getPopupState()
                            .showModalPopup(getState(GuiUtilState.class)
                                    .buildPopup("[" 
                                            + fieldTestName.getText() 
                                            + "] has already been activated. "
                                            + "Change the test name or remove the existing test before proceeding.", 400));
                    return;
                } else {
                    testName = fieldTestName.getText();
                }
                 
                setupTest(agent, size, distance, flags, obstacleAvoidanceType, startPos, testName);
            }

            //Validate user input for float fields.
            private boolean isNumeric(String str) {
                NumberFormat formatter = NumberFormat.getInstance();
                ParsePosition pos = new ParsePosition(0);
                formatter.parse(str, pos);
                return str.length() == pos.getIndex();
            }
        });
        
        contAgentGen.addChild(contRow6, "span");
    }

    @Override
    protected void cleanup(Application app) {
        //Removing this state from StateManager will clear all agents from the 
        //physics space and gui node.
        //from the StateManager.
        Iterator<Map.Entry<String, Test>> iterator = mapTests.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Test> entry = iterator.next();
            List<Node> agents = entry.getValue().getListAgents();
            for (Node agent: agents) {
                getStateManager().getState(BulletAppState.class).getPhysicsSpace().remove(agent);
                ((SimpleApplication) getApplication()).getRootNode().detachChild(agent);
            }
            iterator.remove();
        }
    }

    @Override
    protected void onEnable() {
        getStateManager().attach(new CrowdGenState());
    }

    @Override
    protected void onDisable() {
        getStateManager().detach(this);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void update(float tpf) {
                    
        //Look for incactive test to activate. Loads the agents into the physics
        //space and attaches them to the rootNode.
        if (newTest) {
            mapTests.forEach((key, value)-> {
                if (!value.isActiveTest()) {
                    List<Node> agents = value.getListAgents();
                    for (Node agent: agents) {
                        getStateManager().getState(BulletAppState.class).getPhysicsSpace().add(agent);
                        ((SimpleApplication) getApplication()).getRootNode().attachChild(agent);
                    }
                    value.setActiveTest(true);
                    listBoxTests.getModel().add(key);
                }
            });
            newTest = false;
        }
        
        //Look for tests to remove. Removes the agents from the root node and 
        //physics space. Use iterator to avoid ConcurrentModificationException.
        if (checkTests) {
            Iterator<Map.Entry<String, Test>> iterator = mapTests.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Test> entry = iterator.next();
                if (entry.getValue().isRemoveTest()) {
                    List<Node> agents = entry.getValue().getListAgents();
                    for (Node agent: agents) {
                        getStateManager().getState(BulletAppState.class).getPhysicsSpace().remove(agent);
                        ((SimpleApplication) getApplication()).getRootNode().detachChild(agent);
                    }
                    iterator.remove();
                    Integer selection = listBoxTests.getSelectionModel().getSelection();
                    if (selection != null) {
                        listBoxTests.getModel().remove((int) selection);
                    }
                }
            }
            checkTests = false;
        }
    }
    
    //Set the test parameters for this test.
    private void setupTest(String character, int size, float distance, int updateFlags, 
            int obstacleAvoidanceType, Vector3f startPos, String testName) {
        
        LOG.info("setupTest(agent [{}], size [{}], distance [{}], updateFlags[{}], "
                + "obstacleAvoidanceType [{}], startPos {}, testName [{}])", 
                character, size, distance, updateFlags, obstacleAvoidanceType, startPos, testName);
        
        List<Node> listAgents = new ArrayList<>();
        
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Node agent = (Node) getApplication().getAssetManager().loadModel(character);
                
                agent.setName(testName + "_r" + i + "_c"+ j);
                
                //Set Crowd parameters for the agent
                CrowdAgentControl cac = new CrowdAgentControl();
                cac.setFlags(updateFlags);
                cac.setObstacleAvoidanceType(obstacleAvoidanceType);
                
                //Set the start position for each spatial
                float startX = startPos.getX() + i * distance;
                float startY = startPos.getY();
                float startZ = startPos.getZ() + j * distance;
                Vector3f start = new Vector3f(startX, startY, startZ);
                agent.setLocalTranslation(start);
                
                //The auto generation is based off model bounds and assumes a 
                //bipedal character with a T-Pose as one of the available poses 
                //for the armature. The general assumption being the bounding 
                //box is based on arm length. We want to set the collision shape 
                //of BCC to be around shoulder width and as near to the actual 
                //head height as possible. This would typically mean the largest 
                //number in the x or z direction would be the arm spread. If this 
                //doesn't work we may have to add another parameter to the method 
                //to get the detail desired.
                BoundingBox bounds = (BoundingBox) agent.getWorldBound();
                float x = bounds.getXExtent();
                float y = bounds.getYExtent();
                float z = bounds.getZExtent();
                float xz = x < z ? x:z;
                
                //Give the agent movement controls.
                agent.addControl(new BetterCharacterControl(xz/2, y*2, 20f));
                agent.addControl(new NavMeshChaserControl());
                
                //Add to agents list.
                listAgents.add(agent);
            }
        }
        //Create test and add to the mapTest. Tell the update loop to check for 
        //new tests to activate.
        Test test = new Test(testName, listAgents);
        addMapTest(testName, test);
        newTest = true;
    }
    
    //Explains the settings for the Test Generator.
    private void showLegend() {
        String msg = 
                  "Test Name = name used for the test. The agents used in "
                + "the test will all be named according to their insertion point "
                + "into the grid of agents. This is a required setting.\n\n"
                
                + "Example: In a size 3 grid (3x3 = 9 agents), the naming is as "
                + "follows where r = row, c = column, \n\n"
                
                + "First Row  = testName_r0_c0, testName_r0_c1, testName_r0_c2\n"
                + "Second Row = testname_r1_c0, testName_r1_c1, testName_r1_c2\n"
                + "Third Row  = testname_r2_c0, testName_r2_c1, testName_r2_c2\n\n" 
                
                + "Crowd Separation = spacing between agents in the grid. Must "
                + "be a setting of 0.1f or larger.\n\n"
                
                + "Agent = model to use for the test.\n\n"
                
                + "Crowd Size = number of agents to place into the test in rows "
                + "and columns. Limit = 15 which is 225 agents.\n\n"
                
                + "Example: 2 = 2 rows, 2 columns = a grid of 4 agents\n\n"
                
                + "Avoidance = This is the ObstacleAvoidanceParams "
                + "number you setup when instantiating the crowd. "
                + "Currently, the max number of avoidance types that can be "
                + "set for the Crowd is eight.\n\n"
                
                + "Update Flags = Crowd agent update flags. This is a required "
                + "setting.\n\n"
                
                + "Start Position = starting position the agents will spread out "
                + "evenly from to form the grid. "
                + "This is only used to generate the agents for the test so you "
                + "can drop your agents from above the navMesh if you wish. "
                + "The actual starting point of the agent is determined by its "
                + "final position on the navMesh.\n\n"
                
                + "Active Tests = a list of all the currently active tests. To "
                + "remove a test just select it in the list and press the "
                + "\"Remove Test\" button. Each test must have a unique name. "
                + "Spaces count in the naming so if there is added space after a "
                + "test name, that test will be considered unique.";
        
        Container window = new Container(new MigLayout("wrap"));
        Label label = window.addChild(new Label(msg));
        label.setMaxWidth(400);
        label.setColor(ColorRGBA.Green);
        window.addChild(new ActionButton(new CallMethodAction("Close", window, "removeFromParent")), "align 50%");
        getState(GuiUtilState.class).centerComp(window);
        GuiGlobals.getInstance().getPopupState().showPopup(window);
    }

    /**
     * Add a new test to the mapTest.
     * 
     * @param key the key for the test which is the name of the test.
     * @param value The test to be added to the mapTest.
     */
    private void addMapTest(String key, Test value) {
        mapTests.put(key, value);
    }
    
    private void removeTest() {
        
        //Get the selection from listBoxTests.
        Integer selection = listBoxTests.getSelectionModel().getSelection();
        
        //Check to make sure a test has been selected.
        if (selection == null) {
            GuiGlobals.getInstance().getPopupState()
                    .showModalPopup(getState(GuiUtilState.class)
                            .buildPopup("You must select a test before it can be removed.", 0));
            return;
        }
        
        //Get the tests name from the listBoxTests selection.
        String testName = listBoxTests.getModel().get(selection).toString();
        
        //We check mapTests to see if the key exists. If not, go no further.
        if (!mapTests.containsKey(testName)) {
            GuiGlobals.getInstance().getPopupState()
                    .showModalPopup(getState(GuiUtilState.class)
                            .buildPopup("No test found by that name.", 0));
            return;
        }
        
        //We have a valid test so set the test to be removed and tell the update
        //loop to look for tests to be removed.
        mapTests.get(testName).setRemoveTest(true);
        checkTests = true;
    }

    /**
     * @return the contAgentGen
     */
    public Container getContAgentGen() {
        return contAgentGen;
    }
    
    //The test object for storing the tests. The test name and listAgents are 
    //used to guarentee this is a unique test for the value used for the hashmap.
    private class Test {

        private final String testName;
        private final List<Node> listAgents;
        private boolean activeTest;
        private boolean removeTest;
        
        public Test(String testName, List<Node> listAgents) {
            this.testName = testName;
            this.listAgents = listAgents;
        }
        
        /**
         * If true, this test is scheduled for removal in the next update.
         * 
         * @return the checkTests
         */
        public boolean isRemoveTest() {
            return removeTest;
        }

        /**
         * A setting of true will trigger the removal of the test. All agents 
         * associated with the test will be removed from the physics space and
         * rootNode. Removal takes place in the next pass of the update loop.
         * 
         * @param removeTest the checkTests to set
         */
        public void setRemoveTest(boolean removeTest) {
            this.removeTest = removeTest;
        }
        
        /**
         * If test is inactive it will be activated on the next update and all 
         * agents loaded into the rootNode and physics space from the update loop.
         * 
         * @return the activeTest
         */
        public boolean isActiveTest() {
            return activeTest;
        }

        /**
         * A setting of true will keep the test active in the mapTest and prevent
         * the loading of this tests agents.
         * 
         * @param activeTest the activeTest to set
         */
        public void setActiveTest(boolean activeTest) {
            this.activeTest = activeTest;
        }

        /**
         * The name of this test.
         * 
         * @return the testName
         */
        public String getTestName() {
            return testName;
        }

        /**
         * The list of agents to be used for this crowd test.
         * 
         * @return the listAgents
         */
        public List<Node> getListAgents() {
            return listAgents;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 11 * hash + Objects.hashCode(this.testName);
            hash = 11 * hash + Objects.hashCode(this.listAgents);
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
            final Test other = (Test) obj;
            if (!Objects.equals(this.testName, other.testName)) {
                return false;
            }
            if (!Objects.equals(this.listAgents, other.listAgents)) {
                return false;
            }
            return true;
        }
        
        @Override
        public String toString() {
            return "Test [name = "+ testName + "] " + "AGENTS " + getListAgents(); 
        }

    }

}
