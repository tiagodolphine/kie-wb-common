Stunner - BPMN2 EMF/XMI
=======================

Custom EMF model classes for the jBPM BPMN2 domain.

This module is isolated from Stunner API and the Stunner's BPMN2 domain model.

HowTo update model classes
-------------------------

1.- mvn clean -Pclean-generated-models
    It removes the existing code previously generated from models 
    - org.eclipse.bpmn2.* (BPMN20.genmodel)
    - org.eclipse.dd.* (BPMN20.genmodel)
    - org.jboss.drools.* (bpmn2emfextmodel.genmodel)
    - bpsim.* (bpsim.genmodel)
    - org.omg.spec.bpmn.non.normative.color.* (bpmn2color.genmodel)
    (PLEASE manage properly the git updates)

3.- Generate models in Eclipse
    3.1 - Configure Eclipse workspace
        - Install EMF support plugins in Eclipse
        - File -> Import -> Projects from Folder or Archive
        - Choose directory ->  use _kie-wb-common-stunner-bpmn-emf_ as the root folder
    3.2 - Apply the excepted model updates as well, if any (in ecore and genmodel files) 
    3.3 - Generate models - Right click -> Generate model code 
        - BPMN20.genmodel
        - bpmn2emfextmodel.genmodel
        - bpsim.genmodel
        - bpmn2color.genmodel
    3.4 - Remove the generated gwt.xml module files (for each above genmodel)
    3.5 - Reformat code according our KIE styles
    3.6 - Update licenses/copy-rights
    (PLEASE manage properly the git updates)
    
4.- Build & test & run

5.- Commit (new) generated classes