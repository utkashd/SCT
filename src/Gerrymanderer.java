import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Gerrymanderer {
	
	public static void main(String[] args) {
		
		int numSupports = 13;
		Gerrymanderer gerry = new Gerrymanderer();
		SimplexPDF f = new SimplexPDF(numSupports);
		f.generateRandom(numSupports);
		SimplexPDF g = gerry.solveDiscrete(f);
		
		System.out.println("Original PDF:\n" + f);
		System.out.println("Gerrymandered PDF:\n" + g);
		
	}
	
	/**
	 * An ordered list of real-valued points in [0, 1] from which parties will be generated
	 */
	private ArrayList<Double> partyPoints;
	
	/**
	 * Constructs a Gerrymanderer instance with no default for this.partyPoints
	 */
	Gerrymanderer() {
		this.partyPoints = new ArrayList<Double>();
	}
	
	SimplexPDF solveDiscrete(SimplexPDF f) {
		SimplexPDF bestParties = f;
		SimplexPDF g = null;
		double bestExpectedDistortion = f.getExpectedDistortion();
		double gExpectedDistortion = bestExpectedDistortion;
		Set<Set<Double>> subsetsOfF = getSubsets(f.getPDF().keySet());
		Set<Set<Double>> subsetsOfFOfSizeGEQ3 = new HashSet<Set<Double>>(subsetsOfF);
		for (Set<Double> subset : subsetsOfF) {
			if (subset.size() <= 2) {
				subsetsOfFOfSizeGEQ3.remove(subset);
			}
		}
		for (Set<Double> discretePartyPoints : subsetsOfFOfSizeGEQ3) {
			this.setPartyPoints(discretePartyPoints);
			g = this.gerrymander(f);
			gExpectedDistortion = g.getExpectedDistortion();
			if (gExpectedDistortion > bestExpectedDistortion) {
				bestParties = g;
				bestExpectedDistortion = gExpectedDistortion;
			}
		}
		return bestParties;
	}
	
	SimplexPDF solveDiscrete(SimplexPDF f, int numParties) {
		SimplexPDF bestParties = f;
		SimplexPDF g = null;
		double bestExpectedDistortion = f.getExpectedDistortion();
		double gExpectedDistortion = bestExpectedDistortion;
		Set<Set<Double>> subsetsOfFOfSizeK = getSubsetsOfSizeK(f.getPDF().keySet(), numParties);
		for (Set<Double> discretePartyPoints : subsetsOfFOfSizeK) {
			this.setPartyPoints(discretePartyPoints);
			g = this.gerrymander(f);
			gExpectedDistortion = g.getExpectedDistortion();
			if (gExpectedDistortion > bestExpectedDistortion) {
				bestParties = g;
				bestExpectedDistortion = gExpectedDistortion;
			}
		}
		return bestParties;
	}
	
	/**
	 * Uses bitwise masking to return the power set of a given set. Includes all 2^n sets
	 * @param supports The set to get all subsets of
	 * @return The power set of the parameter supports
	 */
	private Set<Set<Double>> getSubsets(Set<Double> supports) {
		ArrayList<Double> orderedSupports = new ArrayList<Double>(supports);
		Set<Set<Double>> subsets = new HashSet<Set<Double>>();
		for (int i = 0; i < (1 << supports.size()); i++) {
			Set<Double> subset = new HashSet<Double>();
			// use bitmask to determine which elements are put in this subset
			for (int j = 0; j < supports.size(); j++) {
				if (((i >> j) & 1) == 1) {
					subset.add(orderedSupports.get(j));
				}
			}
			subsets.add(subset);
		}
		return subsets;
	}
	
	private Set<Set<Double>> getSubsetsOfSizeK(Set<Double> supports, int k) {
		return this.getSubsetsOfSizeK(supports, k, new HashSet<Set<Double>>());
	}
	
	private Set<Set<Double>> getSubsetsOfSizeK(Set<Double> elements, int k, Set<Set<Double>> listOfSubsets) {
		if (k == 0) {
			// return the set containing the empty set
			listOfSubsets.add(new HashSet<Double>());
			return listOfSubsets;
		}
		for (double el : elements) {
			Set<Double> elementsMinusEl = new HashSet<Double>(elements);
			elementsMinusEl.remove(el);
			for (Set<Double> subsetOfSizeKMinus1 : this.getSubsetsOfSizeK(elementsMinusEl, k-1, new HashSet<Set<Double>>())) {
				subsetOfSizeKMinus1.add(el); // subsetOfSizeKMinus1 is now a subset of size k
				listOfSubsets.add(subsetOfSizeKMinus1);
			}
		}
		return listOfSubsets;
	}
	
	/**
	 * Given a SimplexPDF f and a number of parties to gerrymander f, approximates the party selection that maximizes gerrymandered distortion and returns the corresponding gerrymandered SimplexPDF
	 * @param f The SimplexPDF to gerrymander
	 * @param numParties The number of parties to create
	 * @return The gerrymandered SimplexPDF that had the highest expected distortion of the 1000 random SimplexPDFs tested
	 */
	SimplexPDF solveRandom(SimplexPDF f, int numParties) {
		SimplexPDF bestParties = f;
		for (int i = 0; i < 1000; i++) {
			this.generateRandomParties(numParties);
			SimplexPDF g = gerrymander(f);
			if (g.getExpectedDistortion() > bestParties.getExpectedDistortion()) {
				bestParties = g;
			}
		}
		return bestParties;
	}
	
	/**
	 * Given a SimplexPDF f, returns a gerrymandered version of f with the precondition that this.partyPoints has points
	 * @param f The SimplexPDF to gerrymander
	 * @return The gerrymandered SimplexPDF
	 */
	SimplexPDF gerrymander(SimplexPDF f) {
		SimplexPDF g = new SimplexPDF();
		if (this.partyPoints.isEmpty()) {
			return null;
		}
		for (double partyPoint : this.partyPoints) {
			g.getPDF().put(partyPoint, 0.);
		}
		for (double supportPoint : f.getPDF().keySet()) {
			// first find point in partyPoints nearest to supportPoint
			double nearestPoint = this.partyPoints.get(0);
			for (double partyPoint : this.partyPoints) {
				if (Math.abs(partyPoint - supportPoint) < Math.abs(nearestPoint - supportPoint)) {
					nearestPoint = partyPoint;
				}
			}
			g.getPDF().put(nearestPoint, g.getPDF().get(nearestPoint) + f.getProbability(supportPoint));
		}
		return g;
	}
	
	/**
	 * Populates this.partyPoints with numParties # of points chosen uniformly at random from unit 1-simplex
	 * @param numParties The number of parties to create
	 * @return this.partyPoints
	 */
	ArrayList<Double> generateRandomParties(int numParties) {
		this.partyPoints.clear();
		double partyPoint = Math.random();
		for (int i = 0; i < numParties; i++) {
			while (this.partyPoints.contains(partyPoint)) {
				partyPoint = Math.random();
			}
			this.partyPoints.add(partyPoint);
		}
		Collections.sort(this.partyPoints);
		return this.partyPoints;
	}
	
	/**
	 * Getter for this.partyPoints
	 * @return this.partyPoints
	 */
	ArrayList<Double> getPartyPoints() {
		return this.partyPoints;
	}
	
	/**
	 * Setter for this.partypoints
	 * @param new ArrayList<Double>(discretePartyPoints) The new value for this.partyPoints
	 */
	void setPartyPoints(Set<Double> discretePartyPoints) {
		this.partyPoints = new ArrayList<Double>(discretePartyPoints);
		Collections.sort(this.partyPoints);
	}
	
}
