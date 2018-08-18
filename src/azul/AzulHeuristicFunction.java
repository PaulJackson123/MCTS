package azul;

import main.Board;
import main.Move;
import main.support.HeuristicFunction;

public class AzulHeuristicFunction implements HeuristicFunction {

	private double expCoef;

	AzulHeuristicFunction(double expCoef) {
		this.expCoef = expCoef;
	}

	@Override
	public double h(Board board, Move move) {
		return move instanceof  AzulPlayerMove ? ((Azul) board).getHeuristic((AzulPlayerMove) move) : 1.0;
	}
}
