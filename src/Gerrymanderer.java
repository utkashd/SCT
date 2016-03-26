import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Gerrymanderer {
	
	public static void main(String[] args) {
		
		int numSupports = 10;
		Gerrymanderer gerry = new Gerrymanderer();
		SimplexPDF f = new SimplexPDF(numSupports);
		f.generateRandom(numSupports);
		SimplexPDF tempG = f, g = f;
		double maxDistortion = 1.;
		for (int i = 1; i < numSupports; i++) {
			tempG = gerry.solveDiscrete(f, i);
			// note: if I want to go through every subset
			// I should write a different method for it,
			// as with this way it is really inefficient
			// (getSubsetOfSizeK etc repeats work)
			double expectedDistortionOfG = tempG.getExpectedDistortion();
			if (expectedDistortionOfG > maxDistortion) {
				maxDistortion = expectedDistortionOfG;
				g = tempG;
			}
		}
		
		System.out.println("Original PDF:\n" + f);
		System.out.println("Gerrymandered PDF:\n" + g);
		
		/*
		int pdfSupportSize = 7;
		int partySupportSize1 = 3;
		int partySupportSize2 = 6; // > partySupportSize1
		
		Gerrymanderer gerry = new Gerrymanderer();
		double maxDist = 1.;
		SimplexPDF eff = null, gee1 = null, gee2 = null;
		for (int i = 0; i < 50; i++) {
			SimplexPDF f = new SimplexPDF(pdfSupportSize);
			SimplexPDF g1 = gerry.solveDiscrete(f, partySupportSize1);
			SimplexPDF g2 = gerry.solveDiscrete(f, partySupportSize2);
			if (g1.getExpectedDistortion() < g2.getExpectedDistortion() && !g2.getPDF().values().contains(0.0) && g1.getSupportSize() != pdfSupportSize && g2.getSupportSize() != pdfSupportSize) {
				if (g2.getExpectedDistortion() > maxDist) {
					eff = f;
					gee1 = g1;
					gee2 = g2;
				}
			}
		}
		if (eff != null) {
			System.out.println("Original pdf\n" + eff);
			System.out.println("Gerrymandered pdf with " + partySupportSize1 + " parties\n" + gee1);
			System.out.println("Gerrymandered pdf with " + partySupportSize2 + " parties\n" + gee2);
		}*/
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
	
	SimplexPDF solveDiscrete(SimplexPDF f, int numParties) {
		SimplexPDF bestParties = f;
		Set<Set<Double>> subsetsOfF = getSubsetsOfSizeK(f.getPDF().keySet(), numParties);
		for (Set<Double> discretePartyPoints : subsetsOfF) {
			this.setPartyPoints(discretePartyPoints);
			SimplexPDF g = this.gerrymander(f);
			if (g.getExpectedDistortion() > bestParties.getExpectedDistortion()) {
				bestParties = g;
			}
		}
		return bestParties;
	}
	
	private Set<Set<Double>> getSubsetsOfSizeK(Set<Double> supports, int k) {
		return this.getSubsetsOfSizeK(supports, k, new HashSet<Set<Double>>());
	}
	
	private Set<Set<Double>> getSubsetsOfSizeK(Set<Double> elements, int k, Set<Set<Double>> listOfSubsets) {
		if (k == 0) {
			// return the empty set containing the empty set
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
