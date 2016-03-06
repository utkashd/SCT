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
	
	public SimplexPDF normalize() { // TODO fix
		if (this.supports.lastKey() != 1. || this.supports.firstKey() != 0.) {
			TreeMap<Double, Double> newSupports = new TreeMap<Double, Double>();
			for (double supportPoint : this.supports.keySet()) {
				newSupports.put((supportPoint-this.supports.firstKey())/(this.supports.lastKey() - this.supports.firstKey()), this.supports.get(supportPoint));
			}
			this.supports = newSupports;
		}
		return this;
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
	
	SimplexPDF generateRandom(int numSupports) {
		this.supports.clear();
		double initProbability = Math.random();
		while (initProbability == 0.) {
			initProbability = Math.random();
		}
		this.supports.put(0., initProbability);
		initProbability = Math.random();
		while (initProbability == 0.) {
			initProbability = Math.random();
		}
		this.supports.put(1., initProbability);
		double totalProbability = this.getProbability(0) + this.getProbability(1);
		for (int i = 0; i < numSupports-2; i++) {
			double supportPoint = Math.random();
			while (supportPoint == 0.) {
				supportPoint = Math.random();
			}
			double probability = Math.random();
			while (probability == 0.) {
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

}
