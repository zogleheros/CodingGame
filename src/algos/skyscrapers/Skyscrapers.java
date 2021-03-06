package algos.skyscrapers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** 
 * Recursive algorithm brute-forcing possible values following a carefully selected path.
 * */
public class Skyscrapers {

	private static final Integer SIZE = 7;
	private static final Integer NB_SOLUTIONS = 2;

	static int[][] solvePuzzle () {
		return new Skyscrapers().solve();
	}

	Table table;

	int[][][] solutions = new int[NB_SOLUTIONS][SIZE][SIZE];
	int currentSolutionNumber;

	/**
	 * High-level method which initializes the table object and start the recursion.  
	 * @param clues
	 * @return the solution
	 */
	private int[][] solve() {
		table = new Table();
		Optional<Position> initPosition = table.getNextPosition();
		if (!initPosition.isPresent() || iter(initPosition.get())) {
			return table.table;
		} else {
			throw new RuntimeException("No solution found");
		}
	}

	/**
	 * Core of the Algorithm. <br>
	 * For a given position, we get all the possible values compatible with the rules. 
	 * Then we take the first one and call the same method for the next position.<br>
	 * If the method returns true, we return true to end the recursion.
	 * If there are no next positions, it means we have a solution and thus return true 
	 * (for the last position, the first value is the right one).
	 * If the method returns false, it means the value is wrong : we reset the value 
	 * (and the next position) and try with the next possible value.
	 * If there are no possible values or no possible value returns true, we return false 
	 * as it means we're in a dead-end  and a former value is wrong. 
	 *  
	 * @param position
	 * @return true : if there is no next position or possible value returns true.<br>
	 * 			false : if no possible value returns true
	 * @throws EndException 
	 */

	private void addSolution() throws EndException {
		solutions[currentSolutionNumber] = deepCopy2dArray(table.table);
	}

	private int[][] deepCopy2dArray(int[][] table) {
		int[][] copy = new int[SIZE][SIZE];
		IntStream.range(0, SIZE).forEach(i -> copy[i] = Arrays.copyOf(table[i], SIZE));
		return copy;
	}

	private boolean iter(Position position) {
		Set<Integer> possibleValues = table.getPossibleValuesForPosition(position);
		for (Integer possibleValue : possibleValues) {
			table.setValueForPosition(position, possibleValue);
			Optional<Position> nextPosition = table.getNextPosition();
			if (!nextPosition.isPresent()) {
				System.out.println(table);
				solutions[currentSolutionNumber] = deepCopy2dArray(table.table);
				if (++currentSolutionNumber == NB_SOLUTIONS) {
					return true;
				} else {
					table.resetValue(position);
				}
			}
			if(iter(nextPosition.get())) {
				return true;
			} else {
				table.resetPosition(nextPosition.get());
				table.resetValue(position);
			}
		}
		return false;
	}

	/**
	 * Custom exception thrown when iteration is over
	 */
	@SuppressWarnings("serial")
	public static class EndException extends Exception { }

	/**
	 * Class representing the 2d position (pretty self-explanatory)
	 */
	public static class Position {
		// Integers so that Position is immutable (always a good practice)
		Integer x;
		Integer y;

		Position(Integer x, Integer y) {
			this.x = x;
			this.y = y;
		}

		public Integer getX() {
			return x;
		}

		public Integer getY() {
			return y;
		}

		@Override
		public String toString() {
			return "(" + x + "," + y + ")";
		}

		@Override
		public boolean equals(Object object) {
			if (!(object instanceof Position)) {
				return false;
			}
			Position p2 = (Position) object;
			return x == p2.x && y == p2.y;
		}

	}

	/**
	 * Table is an abstraction of the table, and of the lines it contains. 
	 * This is where we can initialize the table with the given clues, and where we can get the relevant positions
	 * and values for the iteration process. 
	 */
	public static class Table {

		int[][] table = new int[SIZE][SIZE];
		Line[] horizontalLines = new Line[SIZE];
		Line[] verticalLines = new Line[SIZE];
		Deque<Position> positionsQueue = new LinkedList<>();

		Table() {
			initPositionsQueue();
			initLinesWithClues();
		}

		/**
		 * Initialization of the path followed by the iteration process.
		 * We start by the lines with the clues of highest value, so that we can eliminate 
		 * wrong possibilities as fast as possible. <br>
		 * Choosing the path is what makes this algorithm work, if the path is determined randomly, 
		 * the algorithm can take up to 3000 times more time to complete (300s instead of 100ms for test example 3). 
		 * @param clues
		 */
		private void initPositionsQueue() {
			for (int x = 0 ; x < SIZE ; x++) {
				for (int y = 0 ; y < SIZE ; y++) {
					addIfNotPresent(new Position(x, y));
				}
			}
		}

		private void addIfNotPresent(Position position) {
			if (!positionsQueue.contains(position)) {
				positionsQueue.add(position);
			}	
		}

		/** Initialization of the horizontal and vertical lines of the table, and resolve 'init' clues. 
		 * @param clues
		 */
		private void initLinesWithClues() {
			IntStream.range(0, SIZE).forEach(x -> verticalLines[x] = new Line());
			IntStream.range(0, SIZE).forEach(y -> horizontalLines[y] = new Line());
		}

		public void resetPosition(Position position) {
			positionsQueue.addFirst(position);
		}

		public void resetValue(Position position) {
			setValueForPosition(position, 0);
		}

		/**
		 * Getter for the position (x, y). Helps because x and y are reversed, and we shouldn't care about that 
		 * (also prevents from making mistakes as reversing x and y is pretty error-prone).
		 * @param x
		 * @param y
		 * @return
		 */
		private Integer getValueForPosition(int x, int y) {
			return table[y][x]; 	// y and x are inverted so that table[0] gives the first row (simple transposition) 
		}

		private Integer getValueForPosition(Position p) {
			return getValueForPosition(p.getX(), p.getY());  
		}

		/**
		 * Updates value in the table, and updates the relevant lines to ensure consistency of the model.
		 * Like the getter, hides the transposition from the user (it only appear once in the getter and once in the setter).
		 * @param position : position in the table
		 * @param value : value to update with
		 */
		public void setValueForPosition(Position position, int value) {
			int x = position.getX();
			int y = position.getY();
			table[y][x] = value;  	// y and x are inverted so that table[0] gives the first row (simple transposition) 
			verticalLines[x].line[y] = value; 
			horizontalLines[y].line[x] = value; 
		}

		/**
		 * Gets the next null position in the table, with x ascending then y ascending.
		 * @param the current position
		 * @return the next position
		 * @throws EndException : when there is no next position
		 */
		public Optional<Position> getNextPosition() {
			Position nextPosition;
			do {
				nextPosition = positionsQueue.poll();
				if (nextPosition == null) {
					Optional.empty();
				}
			} while (getValueForPosition(nextPosition) != 0);
			return Optional.of(nextPosition);
		}

		/**
		 * Gets the possible values for the given position, by taking the intersection of the possible values 
		 * for the horizontal line and the vertical line.
		 * @param position
		 * @return
		 */
		public Set<Integer> getPossibleValuesForPosition(Position position) {
			int x = position.getX();
			int y = position.getY();
			Set<Integer> possibleValuesForHorizontalLine = horizontalLines[y].getPossibleValuesForPosition(x); 
			if (!possibleValuesForHorizontalLine.isEmpty()) {
				Set<Integer> possibleValuesForVerticalLine = verticalLines[x].getPossibleValuesForPosition(y);
				possibleValuesForHorizontalLine.retainAll(possibleValuesForVerticalLine);
			}
			return possibleValuesForHorizontalLine;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (int i = 0 ; i < SIZE ; i++) {
				sb.append(Arrays.toString(table[i]));
				sb.append("\n");
			}
			return sb.toString();
		}
	}

	/**
	 * Line is an abstraction that represents a horizontal or vertical line.
	 * It is the adequate (at least to my mind) object in which we can enforce the rules of the game, 
	 * namely a different value for each position in the line and the number of skyCrapers seen from each side of the line.
	 */
	public static class Line {
		
		int[] line = new int[SIZE];
		
		static Set<Integer> possibleValues = IntStream.rangeClosed(1, SIZE).boxed().collect(Collectors.toSet());

		Line() {
		}

		/**
		 * Returns list of possible values for given position.
		 * Technically, it is all the possible values [1,...,SIZE] minus the values already placed on the line,
		 * and minus the values that don't check against the clues (clues are only checked when placing last value in the line)
		 * @param positionInLine : given position
		 * @return list of possible values
		 */
		public Set<Integer> getPossibleValuesForPosition(Integer positionInLine) {
			//LocalDateTime before = LocalDateTime.now();
			if(line[positionInLine] != 0) {
				throw new IllegalArgumentException("Fatal Error : Trying to discover already discovered value !!"
						+ "\n position = " + positionInLine + "\n line : " + this);
			}
			Set<Integer> alreadyPlacedValues = getAlreadyPlacedValues(positionInLine);
			Set<Integer> possibleValues = new TreeSet<>(Line.possibleValues);
			possibleValues.removeAll(alreadyPlacedValues); 
			//possibleValues.removeIf(valueToTest -> !isCompatibleWithClues(valueToTest, positionInLine));
			return possibleValues;
		}

		/** 
		 * Returns a set of all the values already placed elsewhere on the line.
		 * @param positionInLine
		 * @return
		 */
		private Set<Integer> getAlreadyPlacedValues(int positionInLine) {
			Set<Integer> alreadyPlacedValues = new HashSet<Integer>();
			for (int i = 0 ; i < SIZE ; i ++) {
				if (i == positionInLine) { // Skip for target square
					continue;
				}
				if (line[i] != 0) { // If there's already a value, we remove it from possibilities on the line
					alreadyPlacedValues.add(line[i]);
				}
			}
			return alreadyPlacedValues;
		}
	}

	//	@Override
	//	public String toString() {
	//		return "\n line : " + Arrays.toString(line) + "\n topClue = " + topClue + " ; bottomClue = " + bottomClue 
	//				;
	//		//		"\n verticalLines : " + Arrays.toString(verticalLines) +
	//		//	"\n horizontalLines : " + Arrays.toString(horizontalLines) +
	//		//"table = " + Arrays.toString(table[0]) + Arrays.toString(table[1]) + 
	//		//Arrays.toString(table[2]) + Arrays.toString(table[3]) ;
	//	}

	private static int clues[][] = {
			{  3, 2, 2, 3, 2, 1,
				1, 2, 3, 3, 2, 2,
				5, 1, 2, 2, 4, 3,
				3, 2, 1, 2, 2, 4},
			{ 0, 0, 0, 2, 2, 0,
					0, 0, 0, 6, 3, 0,
					0, 4, 0, 0, 0, 0,
					4, 4, 0, 3, 0, 0 },
			{ 0, 3, 0, 5, 3, 4, 
						0, 0, 0, 0, 0, 1,
						0, 3, 0, 3, 2, 3,
						3, 2, 0, 3, 1, 0 }
	};

	public static void printTable(int[][] table) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0 ; i < SIZE ; i++) {
			sb.append(Arrays.toString(table[i]));
			sb.append("\n");
		}
		System.out.println(sb);
	}

	public static void main(String...args) {

		LocalDateTime before = LocalDateTime.now();

		int[][] table1 = Skyscrapers.solvePuzzle();
		printTable(table1);

		LocalDateTime after  = LocalDateTime.now();
		Duration duration = Duration.between(before, after);
		System.out.println("Duree du calcul : " + duration.getSeconds() + "s " + duration.getNano()/1_000_000 + "ms");

		//Line(int[] line, int topClue, int bottomClue) {
		//		Line line = new Line(new int[] {4,2,0,0}, 1 ,2);
		//		Set<Integer> set = line.getPossibleValuesForPosition(2);
		//		System.out.println(line);

		//System.out.println(Arrays.toString(Kata.solvePuzzle(clues[0])));


		//int[] input = new int[] { 1, 2, 3, 4, 5 };
		//	System.out.println(foldArray(input, 2));
		//System.out.println(zeros(100_000));
		//System.out.println(zeros(100_005));
		//System.out.println(zeros2(20_000));
		//System.out.println(zeros2(99_995));
		//System.out.println(zeros2(100_000));
		//System.out.println(zeros2(100_005));
	}

	// System.out.println(x);
}