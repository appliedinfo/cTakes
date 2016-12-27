package org.apache.ctakes.coreference.ae;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.coreference.ae.features.salience.ClinicalFeatureExtractor;
import org.apache.ctakes.coreference.ae.features.salience.GrammaticalRoleFeatureExtractor;
import org.apache.ctakes.coreference.ae.features.salience.MorphosyntacticFeatureExtractor;
import org.apache.ctakes.coreference.ae.features.salience.SemanticEnvironmentFeatureExtractor;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.DataWriter;
import org.cleartk.ml.Feature;
import org.cleartk.ml.Instance;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;

public class MarkableSalienceAnnotator extends CleartkAnnotator<Boolean> {

  List<FeatureExtractor1<Markable>> extractors = new ArrayList<>();
  
  public static AnalysisEngineDescription createDataWriterDescription(
      Class<? extends DataWriter<Boolean>> dataWriterClass,
      File outputDirectory) throws ResourceInitializationException{
    return AnalysisEngineFactory.createEngineDescription(
        MarkableSalienceAnnotator.class,
        CleartkAnnotator.PARAM_IS_TRAINING,
        true,
        DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
        dataWriterClass,
        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
        outputDirectory);
  }
  
  public static AnalysisEngineDescription createAnnotatorDescription(String modelPath) throws ResourceInitializationException{
    return AnalysisEngineFactory.createEngineDescription(
        MarkableSalienceAnnotator.class,
        CleartkAnnotator.PARAM_IS_TRAINING,
        false,
        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
        modelPath);
  }
  
  @Override
  public void initialize(UimaContext context)
      throws ResourceInitializationException {
    super.initialize(context);
    
    extractors.add(new MorphosyntacticFeatureExtractor());
    extractors.add(new GrammaticalRoleFeatureExtractor());
    extractors.add(new SemanticEnvironmentFeatureExtractor());
    extractors.add(new ClinicalFeatureExtractor());
  }
  
  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {
    
    for(Markable markable : JCasUtil.select(jcas, Markable.class)){
      boolean outcome;
      List<Feature> features = new ArrayList<>();
      for(FeatureExtractor1<Markable> extractor : extractors){
        features.addAll(extractor.extract(jcas, markable));
      }
      Instance<Boolean> instance = new Instance<>(features);
      
      if(this.isTraining()){
        outcome = markable.getConfidence() > 0.5;
        instance.setOutcome(outcome);
        this.dataWriter.write(instance);
      }else{
        Map<Boolean,Double> outcomes = this.classifier.score(features);
        markable.setConfidence(outcomes.get(true).floatValue());
      }      
    }
  }
}
