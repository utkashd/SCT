import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;

public class SimplexPDF {
	
	private TreeMap<Double, Double> supports;
	
	/*public static void main(String[] args) {
		SimplexPDF f = new SimplexPDF();
		f.generateRandom(10);
		System.out.println(f);
	}*/
	
	SimplexPDF() {
		this.supports = new TreeMap<Double, Double>();
	}
	
	SimplexPDF(int numSupports) {
		this.supports = new TreeMap<Double, Double>();
		this.generateRandom(numSupports);
	}
	
	SimplexPDF(double[] supportPoints, double[] probabilities) {
		this.supports = new TreeMap<Double, Double>();
		for (int i = 0; i < supportPoints.length; i++) {
			this.supports.put(supportPoints[i], probabilities[i]);
		}
	}
	
	TreeMap<Double, Double> getPDF() {
		return this.supports;
	}
	
	double getProbability(double supportPoint) {
		return this.getPDF().get(supportPoint);
	}
	
	int getSupportSize() {
		return this.supports.size();
	}
	
	public String toString() {
		String toPrint = "";
		double roundedSupportPoint = 0.;
		double roundedProbability = 0.;
		for (double supportPoint : this.supports.keySet()) {
			roundedSupportPoint = Math.round(supportPoint * 10000) / 10000.;
			roundedProbability = Math.round(this.supports.get(supportPoint) * 10000) / 10000.;
			toPrint += roundedSupportPoint + " => " + roundedProbability + "\n";
		}
		toPrint += "Expected distortion: " + this.getExpectedDistortion();
		return toPrint + "\n";
	}
	
	public double getMedian() {
		double cumulativeProbability = 0.;
		for (double supportPoint : this.supports.keySet()) {
			cumulativeProbability += this.supports.get(supportPoint);
			if (cumulativeProbability >= 0.5) {
				return supportPoint;
			}
		}
		return -1.;
	}
	
	public double getWinner(double p1, double p2) {
		return Math.abs(this.getMedian() - p1) < Math.abs(this.getMedian() - p2) ? p1 : p2;
	}
	
	public double getExpectedDistortion() {
		double dist = 0.;
		for (double sp1 : this.supports.keySet()) {
			for (double sp2 : this.supports.keySet()) {
				dist += this.getProbability(sp1) * this.getProbability(sp2) * getDistortion(sp1, sp2);
			}
		}
		return dist;
	}
	
	public double getDistortion(double p1, double p2) {
		if (!this.supports.keySet().contains(p1) || !this.supports.keySet().contains(p2)) {
			return -100.; // it will be obvious that there was an issue
		} else {
			double winner = this.getWinner(p1, p2);
			double sc1 = 0.;
			for (double supportPoint : this.supports.keySet()) {
				sc1 += this.supports.get(supportPoint) * Math.abs(supportPoint - p1);
			}
			double sc2 = 0.;
			for (double supportPoint : this.supports.keySet()) {
				sc2 += this.supports.get(supportPoint) * Math.abs(supportPoint - p2);
			}
			if (p1 == winner && sc1 > sc2) {
				return sc1/sc2;
			} else if (p2 == winner && sc2 > sc1) {
				return sc2/sc1;
			} else {
				return 1;
			}
		}
	}
	
	/**
	 * Generates a random pdf according to the method written below
	 */
	SimplexPDF generateRandom(int numSupports) {
		// generate supports, since it should be independent of the simplex sampling method
		this.generateSupports(numSupports);
		// scale the interval out if necessary, since it the space is easier to analyzing assuming the min support is 0 and the max support is 1
		this.normalize();
		// sample probabilities from the simplex
		this.generateRandomUniformInterval();
		return this;
	}
	
	/**
	 * Generates support points for the pdf, but does not assign valid probabilities to them
	 */
	void generateSupports(int numSupports) {
		this.supports.clear();
		for (int i = 0; i < numSupports; i++) {
			double supportPoint = Math.random();
			while (this.supports.keySet().contains(supportPoint)) {
				supportPoint = Math.random();
			}
			this.supports.put(supportPoint, 0.);
		}
	}

	/**
	 * scales a distribution with support points at x1, x2, ..., xn to 0, ..., 1
	 */
	public SimplexPDF normalize() {
		double supportMin = Collections.min(this.supports.keySet());
		double supportMax = Collections.max(this.supports.keySet());
		if (supportMin == 0. && supportMax == 1.) {
			return this;
		}
		TreeMap<Double, Double> newSupports = new TreeMap<Double, Double>();
		for (double supportPoint : this.supports.keySet()) {
			newSupports.put((supportPoint-supportMin)/(supportMax-supportMin), this.supports.get(supportPoint));
		}
		this.supports = newSupports;
		return this;
	}

	/**
	 * Completes generation of the random pdf using "the" naive approach, after supports are initialized and normalized
	 */
	SimplexPDF generateRandomNormal() {
		double totalProbability = 0.;
		for (double supportPoint : this.supports.keySet()) {
			double probability = Math.random();
			while (probability == 0.) { // strictly > 0 probability
				probability = Math.random();
			}
			this.supports.put(supportPoint, probability);
			totalProbability += probability;
		}
		// normalize the probabilities so they sum to 1
		for (double supportPoint : this.supports.keySet()) {
			this.supports.put(supportPoint, this.supports.get(supportPoint)/totalProbability);
		}
		return this;
	}

	/**
	 * Generates a random pdf using normalized hypercube sampling (drawing from exponential distribution)
	 * @precondition this.generateSupports and this.normalize are called, or the keys for this.supports are defined as desired
	 * @param numSupports The number of supports
	 * @return this SimplexPDF
	 */
	SimplexPDF generateRandomHypercubeSampling() {
		double totalProbability = 0.;
		double prob = 0.;
		for (double supportPoint : this.supports.keySet()) {
			prob = Math.random();
			while (prob == 0.) {
				prob = Math.random();
			}
			prob = (-1) * Math.log(prob);
			this.supports.put(supportPoint, prob);
			totalProbability = totalProbability + prob;
		}
		for (double supportPoint : this.supports.keySet()) {
			this.supports.put(supportPoint, this.supports.get(supportPoint)/totalProbability);
		}
		return this;
	}

	/**
	 * Generates a random pdf using Bayesian bootstrap replication
	 * @precondition this.generateSupports and this.normalize are called, or the keys for this.supports are defined as desired
	 * @param numSupports The number of supports
	 * @return this SimplexPDF
	 */
	SimplexPDF generateRandomUniformInterval() {
		ArrayList<Double> intervals = new ArrayList<Double>();
		intervals.add(0.);
		intervals.add(1.);
		double ip = 0.;
		for (int i = 0; i < this.supports.size()-1; i++) {
			ip = Math.random();
			while (ip == 0. || intervals.contains(ip)) { // strictly greater than 0.0
				ip = Math.random();
			}
			intervals.add(ip);
		}
		Collections.sort(intervals);
		int i = 0;
		for (double supportPoint : this.supports.keySet()) {
			this.supports.put(supportPoint, intervals.get(i+1) - intervals.get(i));
			i++;
		}
		return this;
	}

}
