/**
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
package org.apache.ctakes.temporal.ae.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
//import java.util.logging.Logger;
import java.util.Set;

import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

public class EventPropertyExtractor implements FeatureExtractor1<Annotation> {

	//	private String name;
	private static Integer polarity;

	//  private Logger logger = Logger.getLogger(this.getClass().getName());

	public EventPropertyExtractor() {
		super();
		//		this.name = "EventContextualModality";

	}

	@Override
	public List<Feature> extract(JCas view, Annotation annotation) throws CleartkExtractorException {
		List<Feature> features = new ArrayList<>();

		//1 get event:
		EventMention event = (EventMention)annotation;
		//		if(event.getEvent()!= null && event.getEvent().getProperties() != null){
		//			String contextModal = event.getEvent().getProperties().getContextualModality();
		//			if ( "GENERIC".equals(contextModal) ){
		//				Feature contexmod = new Feature(this.name, contextModal);
		//				features.add(contexmod);
		//				//		  logger.info("found a event: "+ contextModal);
		//			}
		//		}

		Set<Sentence> coveringSents = new HashSet<>();
		coveringSents.addAll(JCasUtil.selectCovering(view, Sentence.class, event.getBegin(), event.getEnd()));


		for(Sentence coveringSent : coveringSents){
			List<EventMention> events = JCasUtil.selectCovered(EventMention.class, coveringSent);
			List<EventMention> realEvents = new ArrayList<>();
			for(EventMention eventa : events){
				// filter out ctakes events
				if(eventa.getClass().equals(EventMention.class)){
					realEvents.add(eventa);
				}
			}
			events = realEvents;
			if( events.size()>0){
				EventMention anchor = events.get(0);
				EventMention end    = events.get(events.size()-1);
				if(event == anchor){
					features.add(new Feature("LeftMostEvent"));
				}else if( event == end){
					features.add(new Feature("RightMostEvent"));
				}
			}
		}

		features.addAll(getEventFeats("mentionProperty", event));

		return features;
	}

	private static Collection<? extends Feature> getEventFeats(String name, EventMention mention) {
		List<Feature> feats = new ArrayList<>();
		//add contextual modality as a feature
		if(mention.getEvent()==null || mention.getEvent().getProperties() == null){
			return feats;
		}
		String contextualModality = mention.getEvent().getProperties().getContextualModality();
		if (contextualModality != null)
			feats.add(new Feature(name + "_modality", contextualModality));

		polarity = mention.getEvent().getProperties().getPolarity();
		if(polarity!=null )
			feats.add(new Feature(name + "_polarity", polarity));
		//    feats.add(new Feature(name + "_category", mention.getEvent().getProperties().getCategory()));//null
		//    feats.add(new Feature(name + "_degree", mention.getEvent().getProperties().getDegree()));//null
		
		return feats;
	}

}
