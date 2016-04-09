import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Gerrymanderer {
	
	public static void main(String[] args) {
		
		int minFSupportSize = 15; // incrementing this makes the simulation take much longer
		int maxFSupportSize = 15; // incrementing this makes the simulation take much longer
		int numOfSims = 1000; // incrementing this will not make the simulation much longer
		int numSupports = (int) (Math.random()*(maxFSupportSize-minFSupportSize+1)) + minFSupportSize;
		SimplexPDF f = new SimplexPDF(numSupports);
		Gerrymanderer gerry = new Gerrymanderer();
		SimplexPDF g = gerry.solveDiscreteBF(f);
		HashMap<Integer, HashMap<Integer, Integer>> numSupportsToMapOfCounts = new HashMap<Integer, HashMap<Integer, Integer>>();
		for (int i = 3; i <= maxFSupportSize; i++) {
			HashMap<Integer, Integer> gSizeToFrequency = new HashMap<Integer, Integer>();
			for (int j = 3; j <= i; j++) {
				gSizeToFrequency.put(j, 0);
			}
			numSupportsToMapOfCounts.put(i, gSizeToFrequency);
		}
		numSupportsToMapOfCounts.get(f.getSupportSize()).put(g.getSupportSize(), numSupportsToMapOfCounts.get(f.getSupportSize()).get(g.getSupportSize()) + 1);
		for (int i = 0; i < numOfSims-1; i++) { // because we've already done one sim
			numSupports = (int) (Math.random()*(maxFSupportSize-minFSupportSize+1)) + minFSupportSize;
			if (numSupports == 3) {
				// ignore it, it doesn't provide any useful information
			} else {
				f.generateRandom(numSupports);
				g = gerry.solveDiscreteBF(f);
				numSupportsToMapOfCounts.get(f.getSupportSize()).put(g.getSupportSize(), numSupportsToMapOfCounts.get(f.getSupportSize()).get(g.getSupportSize()) + 1);
			}
		}
		for (int i = minFSupportSize; i <= maxFSupportSize; i++) { // from 4, because we're ignoring f of size 3
			System.out.print(i);
			for (int j = 3; j <= i; j++) {
				System.out.print("\t");
				if (numSupportsToMapOfCounts.get(i).get(j) > 0) {
					System.out.print(j + ":" + numSupportsToMapOfCounts.get(i).get(j));
				}
			}
			System.out.println();
		}
		
		/*int numSupports = 12;
		Gerrymanderer gerry = new Gerrymanderer();
		SimplexPDF f = new SimplexPDF(numSupports);
		SimplexPDF g = gerry.solveDiscreteBF(f);
		while (g.getPDF().size() <= 7 || g.getExpectedDistortion() - 1 < 0.01) {
			f.generateRandom(numSupports);
			g = gerry.solveDiscreteBF(f);
		}
		System.out.println("Original PDF:\n" + f);
		System.out.println("PDF median: " + f.getMedian());
		System.out.println("\nGerrymandered PDF:\n" + g);*/
		
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
	
	/*SimplexPDF solveDiscrete(SimplexPDF f) {
		
		
		
	}*/
	
	SimplexPDF solveDiscreteBF(SimplexPDF f) {
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
	
	SimplexPDF solveDiscreteBF(SimplexPDF f, int numParties) {
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
