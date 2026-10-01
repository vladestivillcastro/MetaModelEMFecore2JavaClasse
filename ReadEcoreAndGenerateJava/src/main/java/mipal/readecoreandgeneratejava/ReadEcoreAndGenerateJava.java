/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package mipal.readecoreandgeneratejava;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.emf.codegen.ecore.generator.Generator;
import org.eclipse.emf.codegen.ecore.generator.GeneratorAdapterFactory;
import org.eclipse.emf.codegen.ecore.genmodel.GenJDKLevel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelFactory;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelPackage;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.codegen.ecore.genmodel.generator.GenBaseGeneratorAdapter;
import org.eclipse.emf.codegen.ecore.genmodel.generator.GenModelGeneratorAdapterFactory;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.Resource;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
//import org.eclipse.emf.codegen.ecore.genmodel.GenJDKLevel;

/**
 *
 * @author vladimirestivill-castro
 */
public class ReadEcoreAndGenerateJava {

    public static final String ECORE_EXTENSION="ecore";
    public static final String GEN_MODEL_EXTENSION="genmodel";
    
    
    static void printDiagnostic(Diagnostic d, String indent) {
        System.out.println(indent + severityName(d.getSeverity()) + ": " + d.getMessage());

        if (d.getException() != null) {
            d.getException().printStackTrace(System.out);
        }
        for (Diagnostic child : d.getChildren()) {
                printDiagnostic(child, indent + "  ");
        }
    }//printDiagnostic
    
    static String severityName(int severity) {
        switch (severity) {
            case Diagnostic.OK:      return "OK";
            case Diagnostic.INFO:    return "INFO";
            case Diagnostic.WARNING: return "WARNING";
            case Diagnostic.ERROR:   return "ERROR";
            case Diagnostic.CANCEL:  return "CANCEL";
            default:                 return "UNKNOWN(" + severity + ")";
        }
    } //severityName

    
    public static void main(String[] args) {
        System.out.println("(c) 2026 Vladimir Estivill-Castro");
        
        Options options = new Options();
        
        Option op_input_ecore_file = new Option("e", "ecore", true, "name of EMF input ecore file"); // 'true' means it expects an argument
        op_input_ecore_file.setRequired(true); // make it mandatory
        options.addOption(op_input_ecore_file);
        
        Option help = new Option("h", "help", false, "Display help information"); // 'false' for a flag/switch
        options.addOption(help);
        
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd=null;
        
        try {
            cmd = parser.parse(options, args);
            
            if (cmd.hasOption("help")) {
                formatter.printHelp("Experimental  Generator from .ecore", options); // Auto-generate help message
                return;
            }
            
        } catch (ParseException ex) {
            System.err.println(ex.getMessage());
            formatter.printHelp("This program has the following options ", options); // Print help on error
            System.exit(1);
        }
        
        String name_input_genmodel_file=null;
        String project_name = null;
        if (null!= cmd && cmd.hasOption("ecore")) {
            name_input_genmodel_file = cmd.getOptionValue("ecore");
            int index = name_input_genmodel_file.indexOf(".");
            if (index != -1) {
                name_input_genmodel_file = name_input_genmodel_file.substring(0, index); // Keeps everything before the first "."
            }
            project_name=name_input_genmodel_file;
            name_input_genmodel_file+="."+ECORE_EXTENSION;
            System.out.println("The genmodelfile is "+name_input_genmodel_file);
        }
        
        if (null==name_input_genmodel_file) {
            System.err.println("Unkown genmodel file name");
            System.exit(1);
        }
        
        // 1. Load the .ecore
    ResourceSet rs = new ResourceSetImpl();
    rs.getResourceFactoryRegistry().getExtensionToFactoryMap() .put(ECORE_EXTENSION, new EcoreResourceFactoryImpl());
    rs.getResourceFactoryRegistry().getExtensionToFactoryMap() .put(GEN_MODEL_EXTENSION, new XMIResourceFactoryImpl());
    
    Resource ecoreRes = rs.getResource(URI.createFileURI(name_input_genmodel_file), true);
    EPackage ePackage = (EPackage) ecoreRes.getContents().get(0);
    
    // 2. Create and initialize the GenModel
    GenModel genModel = GenModelFactory.eINSTANCE.createGenModel();
    for (GenJDKLevel level : GenJDKLevel.VALUES) {
        System.out.println(level);
    }
    
    
    genModel.setComplianceLevel(GenJDKLevel.JDK70_LITERAL);
    genModel.setModelName("Mipal");
    genModel.setModelPluginID("newGITmetamodelwithtypes");
    genModel.setModelDirectory("/newGITmetamodelwithtypes/src");
    genModel.initialize(Collections.singleton(ePackage));
    
    // 3. Tweak the GenPackage(s)
    GenPackage genPackage = (GenPackage) genModel.getGenPackages().get(0);
    genPackage.setPrefix("NewGITmetamodelwithtypes");               // prefix for generated class names
    genPackage.setBasePackage(""); // Java package prefix
    
    // 4. Save the .genmodel (optional)
    Resource genRes = rs.createResource(URI.createFileURI("Mipal.genmodel"));
    genRes.getContents().add(genModel);
        try {
            genRes.save(Collections.emptyMap());
        } catch (IOException ex) {
            System.getLogger(ReadEcoreAndGenerateJava.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        
    
    File projectDir = new File(System.getProperty("user.dir") + File.separator + "output", project_name);
    projectDir.mkdirs();
    
    // Map the "project" to a real folder (replaces the URI_MAP approach I gave earlier;
    // this is the standard way outside Eclipse)
    EcorePlugin.getPlatformResourceMap().put(project_name, URI.createFileURI(projectDir.getAbsolutePath() + File.separator));
    
    genModel.setModelDirectory("/" + project_name + "/src");   // project-relative, NOT a file path
    genModel.setCanGenerate(true);
    genModel.setUpdateClasspath(false);   // important: otherwise it tries to touch an Eclipse project/.classpath
    genModel.setModelPluginID(project_name);
    
    // --- Register the adapter factory ---
    GeneratorAdapterFactory.Descriptor.Registry.INSTANCE.addDescriptor(
        GenModelPackage.eNS_URI, GenModelGeneratorAdapterFactory.DESCRIPTOR);
        
            // Create the generator
    Generator generator = new Generator();
    generator.setInput(genModel); // Pass your .genmodel object
        
    System.out.println("Before generation");
    Diagnostic d = generator.generate (genModel, GenBaseGeneratorAdapter.MODEL_PROJECT_TYPE, new BasicMonitor.Printing(System.out));
    System.out.println("After generation");
    System.out.println("Severity: " + d.getSeverity());
    System.out.println("Message:  " + d.getMessage());
    printDiagnostic(d, "");
        
        System.out.println("Generation finished");

    }// main
}// class
