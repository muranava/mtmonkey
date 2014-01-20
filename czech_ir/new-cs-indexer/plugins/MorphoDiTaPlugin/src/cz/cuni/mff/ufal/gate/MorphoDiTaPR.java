package cz.cuni.mff.ufal.gate;

import java.net.URL;

import cz.cuni.mff.ufal.morphodita.Forms;
import cz.cuni.mff.ufal.morphodita.TaggedLemma;
import cz.cuni.mff.ufal.morphodita.TaggedLemmas;
import cz.cuni.mff.ufal.morphodita.Tagger;
import cz.cuni.mff.ufal.morphodita.TokenRange;
import cz.cuni.mff.ufal.morphodita.TokenRanges;
import cz.cuni.mff.ufal.morphodita.Tokenizer;
import gate.AnnotationSet;
import gate.Factory;
import gate.FeatureMap;
import gate.Resource;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.util.InvalidOffsetException;

@SuppressWarnings("serial")
@CreoleResource(name = "Czech tagger, morphology, tokenizer and sentence splitter")
public class MorphoDiTaPR extends AbstractLanguageAnalyser {
	
	/** Init-time parameters */
	private URL taggerModelPath;
	
	/**
	 * @return the perlSegmenterPath
	 */
	public URL getTaggerModelPath() {
		return taggerModelPath;
	}

	/**
	 * @param perlSegmenterPath the perlSegmenterPath to set
	 */
	@CreoleParameter(comment = "path to a model for MorphoDiTa tagger",
			defaultValue = "morphodita/models/czech-131023.tagger-best_accuracy")
	public void setTaggerModelPath(URL taggerModelPath) {
		this.taggerModelPath = taggerModelPath;
	}
	
	/** Run-time parameters */
	private String fromRawText;
	
	/**
	 * @return the fromRawText
	 */
	public String getFromRawText() {
		return fromRawText;
	}

	/**
	 * @param fromRawText the fromRawText to set
	 */
	@CreoleParameter(comment = "carry out POS tagging along with tokenization and sentence splitting",
			defaultValue = "true")
	public void setFromRawText(String fromRawText) {
		this.fromRawText = fromRawText;
	}

	Tagger tagger = null;
	
	public Resource init() throws ResourceInstantiationException {
		this.tagger = Tagger.load(this.taggerModelPath.getPath());
		if (tagger == null)
			throw new ResourceInstantiationException();
		return this;
	}

	public void execute() throws ExecutionException {
		if (Boolean.valueOf(getFromRawText())) {
			try {
				tagUntokenized();
			} catch (InvalidOffsetException e) {
				throw new ExecutionException();
			}
		}
		else {
		}
	}
	
	public void tagUntokenized() throws InvalidOffsetException {
	
		Tokenizer tokenizer = this.tagger.newTokenizer();
		Forms forms = new Forms();
	    TaggedLemmas taggedLemmas = new TaggedLemmas();
	    TokenRanges tokens = new TokenRanges();
		
		String text = document.getContent().toString();
		AnnotationSet morphoAnnot = document.getAnnotations("Morpho");
		
		tokenizer.setText(text);
		
		long t = 0;
		while (tokenizer.nextSentence(forms, tokens)) {
			long sentStart = t;
			this.tagger.tag(forms, taggedLemmas);
			for (int i = 0; i < taggedLemmas.size(); i++) {
				TaggedLemma taggedLemma = taggedLemmas.get(i);
				TokenRange token = tokens.get(i);
				long tokenStart = token.getStart();
				long tokenEnd = token.getStart() + token.getLength();
				
				morphoAnnot.add(t, tokenStart, "SpaceToken", Factory.newFeatureMap());
				
				FeatureMap morphoFeats = Factory.newFeatureMap();
				morphoFeats.put("lemma", taggedLemma.getLemma());
				morphoFeats.put("tag", taggedLemma.getTag());
				morphoAnnot.add(tokenStart, tokenEnd, "SpaceToken", morphoFeats);
				
				t = tokenEnd;
			}
			morphoAnnot.add(sentStart, t, "Sentence", Factory.newFeatureMap());
		}
	}
	
}