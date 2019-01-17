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
import com.jme3.recast4j.demo.controls.NavMeshChaserControl;
import com.jme3.recast4j.demo.layout.MigLayout;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.ActionButton;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.CallMethodAction;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.ListBox;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.event.DefaultMouseListener;
import com.simsilica.lemur.event.MouseEventControl;
import com.simsilica.lemur.event.PopupState;
import com.simsilica.lemur.text.DocumentModelFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Robert
 */
public class AgentGridGuiState extends BaseAppState {

    private static final Logger LOG = LoggerFactory.getLogger(AgentGridGuiState.class.getName());

    private DocumentModelFilter doc;
    private ListBox listBoxAgent;
    private ListBox listBoxSize;
    private ListBox listBoxTests;
    private TextField fieldDistance;
    private TextField fieldPosX;
    private TextField fieldPosY;
    private TextField fieldPosZ;
    private TextField fieldTestName;
    private Map<String, Test> mapTests;
    private boolean newTest;
    private boolean checkTests;
    private Container contAgentGridGui;
    private float maxPopupSize;
    
    @SuppressWarnings("unchecked")
    @Override
    protected void initialize(Application app) {
        // Holds all active tests.
        mapTests = new HashMap<>();
        //Set the maximum size a popup can be.
        maxPopupSize = 400.0f;
        
        //The top container for this gui.
        contAgentGridGui = new Container(new MigLayout(null));
        contAgentGridGui.setName("AgentGridGuiState contAgentGridGui");
        contAgentGridGui.setAlpha(0, false);
        
        //Holds all the underlying components for this gui.
        Container contMain = new Container(new MigLayout(null)); 
        contMain.setName("AgentGridGuiState contMain");
        contMain.setAlpha(0, false);
        contAgentGridGui.addChild(contMain);

        //The test name field.
        contMain.addChild(new Label("Test Name"), "split 2");
        doc = new DocumentModelFilter();
        //placeholder for filtering
        fieldTestName = contMain.addChild(new TextField(doc), "growx, wrap");
        fieldTestName.setSingleLine(true);
        fieldTestName.setText("Test");
        
        //The crowd separation field.
        contMain.addChild(new Label("Agent Separation"), "split 3, growx");
        doc = new DocumentModelFilter();
        //placeholder for filtering        
        fieldDistance = contMain.addChild(new TextField(doc));
        fieldDistance.setSingleLine(true);
        fieldDistance.setPreferredWidth(50);
        fieldDistance.setText("1.0");
        contMain.addChild(new Label(".float"), "wrap");        
           
        //The agentPath and crowd size labels. 
        contMain.addChild(new Label("Agent"), "split 2, growx");
        contMain.addChild(new Label("Grid Size"), "wrap");
        
        //The agentPath listbox. Add new model name here. Add the asset path to the 
        //models in butAddAgentGrid listener under the #Model Section.
        listBoxAgent = contMain.addChild(new ListBox(), "split 2, growx");
        listBoxAgent.getModel().add("Jamie");
        listBoxAgent.getSelectionModel().setSelection(0);
        
        //The grid size listbox. 
        listBoxSize = contMain.addChild(new ListBox(), "gapright 10, wrap");
        int size = 1;
        for (int i = 0; i < 15; i++) {
            listBoxSize.getModel().add(size);
            size++;
        }
        listBoxSize.getSelectionModel().setSelection(0);
        
        //The start postion field.
        contMain.addChild(new Label("Start Position"), "wrap"); 
        //X
        contMain.addChild(new Label("X:"), "split 6");
        doc = new DocumentModelFilter();
        //placeholder for filtering
        fieldPosX = contMain.addChild(new TextField(doc));
        fieldPosX.setSingleLine(true);
        fieldPosX.setPreferredWidth(50);
        fieldPosX.setText("0.0");
        //Y
        contMain.addChild(new Label("Y:"));
        doc = new DocumentModelFilter();
        //placeholder for filtering
        fieldPosY = contMain.addChild(new TextField(doc));
        fieldPosY.setSingleLine(true);
        fieldPosY.setPreferredWidth(50);
        fieldPosY.setText("0.0");
        //Z        
        contMain.addChild(new Label("Z:"));
        doc = new DocumentModelFilter();
        //placeholder for filtering        
        fieldPosZ = contMain.addChild(new TextField(doc), "wrap");
        fieldPosZ.setSingleLine(true);
        fieldPosZ.setPreferredWidth(50);
        fieldPosZ.setText("0.0");
        
        //The active tests listbox.
        contMain.addChild(new Label("Active Tests"), "wrap");
        listBoxTests = contMain.addChild(new ListBox(), "growx, wrap");
        //Use the method call to set the checkTests boolean to true.
        contMain.addChild(new ActionButton(new CallMethodAction("Remove Test", this, "removeTest")), "wrap");
        
        //Holds the Generate Agents and Legend buttons.
        Container contButton = new Container(new MigLayout("wrap 2", // Layout Constraints
                "[]push[][]")); // Column constraints [min][pref][max]
        contButton.setName("AgentGridGuiState contButton");
        contMain.addChild(contButton, "growx");
        //Buttons.
        contButton.addChild(new ActionButton(new CallMethodAction("Legend", this, "showLegend")));
        Button butAddAgentGrid = contButton.addChild(new Button("Add Agent Grid"));
        //The listener for butAddAgentGrid button.
        MouseEventControl.addListenersToSpatial(butAddAgentGrid, new DefaultMouseListener() {
            //Default minimums
            private String testName     = "No Name";
            private float separation    = 1.0f;
            private String agentPath    = null;
            private Vector3f startPos   = new Vector3f(0, 0, 0);
            
            @Override
            protected void click( MouseButtonEvent event, Spatial target, Spatial capture ) {
                
                //Garbage in?, no test.
                if (!getState(GuiUtilState.class).isNumeric(fieldDistance.getText()) 
                ||  fieldDistance.getText().isEmpty()) {
                    GuiGlobals.getInstance().getPopupState()
                            .showModalPopup(getState(GuiUtilState.class)
                                    .buildPopup("[ Agent Separation ] requires a valid float value.", 0));
                    return;
                } else {
                    separation = new Float(fieldDistance.getText());
                    //Stop negative separation input for agentPath grid.
                    if (separation < 0.1f) {
                        GuiGlobals.getInstance().getPopupState()
                                .showModalPopup(getState(GuiUtilState.class)
                                        .buildPopup("[ Agent Separation ] requires a float value >= 0.1f.", 0));
                        return;
                    }
                }
                
                //###### Model Section #####
                //Model to use for the test. Selection is set to 0 when creating 
                //the listBoxAgent so shouldn't need to check for null.
                switch(listBoxAgent.getSelectionModel().getSelection()) {
                    case 0: 
                        agentPath = "Models/Jaime/Jaime.j3o";
                        break;
                    default: agentPath = null;
                }
                
                //Number of agents to place into the test in rows and columns.
                //Selection is set to 0 when creating the listBoxSize so 
                //shouldn't need to check for null.
                int size = listBoxSize.getSelectionModel().getSelection() + 1;
                
                //The starting position of the test. Sanity check.
                if (!getState(GuiUtilState.class).isNumeric(fieldPosX.getText()) || fieldPosX.getText().isEmpty() 
                ||  !getState(GuiUtilState.class).isNumeric(fieldPosY.getText()) || fieldPosY.getText().isEmpty() 
                ||  !getState(GuiUtilState.class).isNumeric(fieldPosZ.getText()) || fieldPosZ.getText().isEmpty()) {
                    GuiGlobals.getInstance().getPopupState()
                            .showModalPopup(getState(GuiUtilState.class)
                                    .buildPopup("[ Start Position ] requires a valid float value.", 0));
                    return;
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
                                    .buildPopup("[ " 
                                            + fieldTestName.getText() 
                                            + " ] has already been activated. "
                                            + "Change the test name or remove the existing test before proceeding.", maxPopupSize));
                    return;
                } else {
                    testName = fieldTestName.getText();
                }
                 
                addAgentGrid(agentPath, size, separation, startPos, testName);
            }

            //Set the grid parameters for this test.
            private void addAgentGrid(String agentPath, int size, float distance, Vector3f startPos, String testName) {

                //Anything over 2 arguments creates a new object so split this up.
                LOG.info("<===== Begin AgentGridGuiState addAgentGrid =====>");
                LOG.info("agentPath     [{}]", agentPath);
                LOG.info("size          [{}]", size);
                LOG.info("separation    [{}]", distance);
                LOG.info("startPos      [{}]", startPos);
                LOG.info("testName      [{}]", testName);

                List<Node> listAgents = new ArrayList<>();

                for (int i = 0; i < size; i++) {
                    for (int j = 0; j < size; j++) {
                        Node agent = (Node) getApplication().getAssetManager().loadModel(agentPath);

                        agent.setName(testName + "_r" + i + "_c"+ j);

                        //Set the start position for each spatial
                        float startX = startPos.getX() + i * distance;
                        float startY = startPos.getY();
                        float startZ = startPos.getZ() + j * distance;
                        Vector3f start = new Vector3f(startX, startY, startZ);
                        agent.setLocalTranslation(start);

                        //The auto generation is based off model bounds and assumes a 
                        //bipedal agentPath with a T-Pose as one of the available poses 
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
                        float radius = xz/2;
                        float height = y*2;

                        //Give the agentPath movement controls.
                        agent.addControl(new BetterCharacterControl(radius, height, 20f));
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
                LOG.info("<===== End AgentGridGuiState addAgentGrid =====>");
            }
            
        });
        
    }

    @Override
    protected void cleanup(Application app) {
        //Removing this state from StateManager will clear all agents from the 
        //physics space and gui node. The removal of the gui components is a by
        //product of the removal of CrowdTabsGuiState where this gui lives.
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
        //Start the AgentSettingsGuiState which in turn starts the CrowdTabsGuiState.
        //Called from DemoApplication F1 button ActionListener.
        getStateManager().attach(new AgentSettingsGuiState());
    }

    @Override
    protected void onDisable() {
        //Called by the DemoApplication F1 button ActionListener.
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
    
    //Explains the settings for the Test Generator.
    private void showLegend() {
        String msg = 
                  "Test Name - The name used for the test. The agents used in "
                + "the test will all be named according to their insertion point "
                + "into the grid of agents. This is a required setting.\n\n"
                
                + "Example: In a size 3 grid (3x3 = 9 agents), the naming is as "
                + "follows where r = row, c = column, \n\n"
                
                + "Row1 = testName_r0_c0, testName_r0_c1, testName_r0_c2\n"
                + "Row2 = testname_r1_c0, testName_r1_c1, testName_r1_c2\n"
                + "Row2 = testname_r2_c0, testName_r2_c1, testName_r2_c2\n\n" 
                
                + "Agent Separation - Spacing between agents in the grid. Must "
                + "be a setting of 0.1f or larger.\n\n"
                
                + "Agent - Model to use for the test.\n\n"
                
                + "Grid Size - Number of agents to place into the test in rows "
                + "and columns. Limit = 15 which is 225 agents.\n\n"
                
                + "Example: 2 = 2 rows, 2 columns = a grid of 4 agents\n\n"
                
                + "Start Position - Starting position the agents will spread out "
                + "evenly from to form the grid. This is only used to generate "
                + "the agents for the test so you can drop your agents from above "
                + "the navMesh if you wish. The actual starting point of the agent "
                + "is determined by its final position on the navMesh prior to "
                + "being added to the crowd.\n\n"
                
                + "Active Tests - The list of all the currently active tests. "
                + "Select any test from the list and press the \"Remove Test\" "
                + "button to remove the test and all active agents related to "
                + "that test. Each test must have a unique name. Spaces count in "
                + "the naming so if there is added space after a test name, that "
                + "test will be considered unique.";
        
        Container window = new Container(new MigLayout("wrap"));
        Label label = window.addChild(new Label(msg));
        label.setMaxWidth(maxPopupSize);
        label.setColor(ColorRGBA.Green);
        window.addChild(new ActionButton(new CallMethodAction("Close", window, "removeFromParent")), "align 50%");
        getState(GuiUtilState.class).centerComp(window);
        //This assures clicking outside of the message should close the window 
        //but not activate underlying UI components.
        GuiGlobals.getInstance().getPopupState().showPopup(window, PopupState.ClickMode.ConsumeAndClose, null, null);
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
    
    //Removes a test from mapTests.
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
     * @return the contAgentGridGui.
     */
    public Container getContAgentGridGui() {
        return contAgentGridGui;
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
