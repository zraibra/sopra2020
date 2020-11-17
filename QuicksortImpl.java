package solutions.exercise2;

import java.util.Comparator;

import org.sopra.api.exercises.ExerciseSubmission;
import org.sopra.api.exercises.exercise2.Quicksort;

public class QuicksortImpl<T> implements Quicksort<T>, ExerciseSubmission {

	private Comparator<T> comparator;

	public QuicksortImpl(Comparator<T> comparator) throws IllegalArgumentException {
		if (comparator == null) {
			throw new IllegalArgumentException("Comparator is not allowed to be null.");
		}
		this.comparator = comparator;
	}

	public int partition(T[] arr, int left, int right) {
		if (arr == null || left < 0 || right >= arr.length || left > right) {
			throw new IllegalArgumentException("Either arr is null or borders are invalid.");
		}
		int leftposition = left;
		int rightposition = right;
		int pivotposition = (int) (left + right) / 2;
		T pivot = arr[pivotposition];
		while (leftposition <= rightposition) {
			while (comparator.compare(arr[leftposition], pivot) < 0) {
				leftposition++;

			}
			while (comparator.compare(arr[rightposition], pivot) > 0) { 
				rightposition--;

			}
			if (leftposition <= rightposition) {
				T help = arr[leftposition];
				arr[leftposition] = arr[rightposition];
				arr[rightposition] = help;
				leftposition++;
				rightposition--;
			}
		}
		return leftposition;
	}

	public void quicksort(T[] arr, int left, int right) {
		if (arr == null) {
			throw new IllegalArgumentException("Arr is not allowed to be null");
		}
		if (left < right) {
			int partitioning = partition(arr, left, right);
			quicksort(arr, left, partitioning - 1);
			quicksort(arr, partitioning, right);
		}
	}

	public String getTeamIdentifier() {
		return "G03T03";
	}

}
