/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.relationextractor.pipelines;

import java.io.File;
import java.io.IOException;

import org.apache.ctakes.core.cr.FilesInDirectoryCollectionReader;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.CasIOUtil;
import org.apache.uima.jcas.JCas;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * A simple pipeline that runs relation extraction on all files in a directory and saves
 * the resulting annotations as XMI files. The core part of this pipeline is the aggregate
 * relation extractor AE which runs all the preprocessing that is necessary for relation
 * extraction as well as the AEs that extract relations.
 * 
 * @author dmitriy dligach
 *
 */
public class RelationExtractorPipeline {

  public static class Options {

    @Option(
        name = "--input-dir",
        usage = "specify the path to the directory containing the clinical notes to be processed",
        required = true)
    public String inputDirectory;
    
    @Option(
        name = "--output-dir",
        usage = "specify the path to the directory where the output xmi files are to be saved",
        required = true)
    public String outputDirectory;
  }
  
	public static void main(String[] args) throws UIMAException, IOException, CmdLineException {
		
		Options options = new Options();
		CmdLineParser parser = new CmdLineParser(options);
		parser.parseArgument(args);

		CollectionReaderDescription collectionReader = CollectionReaderFactory.createReaderDescriptionFromPath(
				"../ctakes-core/desc/collection_reader/FilesInDirectoryCollectionReader.xml",
				FilesInDirectoryCollectionReader.PARAM_INPUTDIR,
				options.inputDirectory);

		// make sure the model parameters match those used for training
		AnalysisEngineDescription relationExtractor = AnalysisEngineFactory.createEngineDescriptionFromPath(
				"desc/analysis_engine/RelationExtractorAggregate.xml");
    
		int fileNum = 0;
		for(JCas jcas : SimplePipeline.iteratePipeline(collectionReader, relationExtractor)){
			CasIOUtil.writeXmi(jcas, new File(options.outputDirectory, String.format("%d.txt", fileNum++)));
		}
    
	}
}
