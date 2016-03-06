import java.util.ArrayList;
import java.util.Collections;

public class Gerrymanderer {
	
	public static void main(String[] args) {
		int pdfSupportSize = 5;
		int partySupportSize1 = 3;
		int partySupportSize2 = 4; // > partySupportSize1
		
		Gerrymanderer gerry = new Gerrymanderer();
		double maxDist = 1.;
		SimplexPDF eff = null, gee1 = null, gee2 = null;
		for (int i = 0; i < 50; i++) {
			SimplexPDF f = new SimplexPDF(pdfSupportSize);
			SimplexPDF g1 = gerry.solve(f, partySupportSize1);
			SimplexPDF g2 = gerry.solve(f, partySupportSize2);
			if (g1.getExpectedDistortion() < g2.getExpectedDistortion() && !g2.getPDF().values().contains(0.0) && g1.getSupportSize() != pdfSupportSize && g2.getSupportSize() != pdfSupportSize) {
				if (g2.getExpectedDistortion() > maxDist) {
					eff = f;
					gee1 = g1;
					gee2 = g2;
				}
			}
		}
		if (eff != null) {
			System.out.println(eff);
			System.out.println(gee1);
			System.out.println(gee2);
		}
	}
	
	ArrayList<Double> partyPoints;
	
	Gerrymanderer() {
		this.partyPoints = new ArrayList<Double>();
	}
	
	SimplexPDF solve(SimplexPDF f, int numParties) {
		SimplexPDF bestParties = f;
		for (int i = 0; i < 100; i++) {
			this.generateRandomParties(numParties);
			SimplexPDF g = gerrymander(f);
			if (g.getExpectedDistortion() > bestParties.getExpectedDistortion()) {
				bestParties = g;
			}
		}
		return bestParties;
	}
	
	SimplexPDF gerrymander(SimplexPDF f) {
		SimplexPDF g = new SimplexPDF();
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

}
