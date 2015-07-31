package models;

import pcv.Algorithm;

/**
 * Process Path Pojo
 * 
 * @author Sildu
 *
 */
public class ProcessPath {
	private String		destiny;
	private Algorithm	algorithm;

	public ProcessPath(String destiny, Algorithm algorithm) {
		this.destiny = destiny;
		this.algorithm = algorithm;
	}

	public String getDestiny() {
		return destiny;
	}

	public void setDestiny(String destiny) {
		this.destiny = destiny;
	}

	public Algorithm getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(Algorithm algorithm) {
		this.algorithm = algorithm;
	}

}
