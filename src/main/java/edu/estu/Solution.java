package edu.estu;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable representation of a candidate or optimal solution to a {@link KP01} instance.
 *
 * @param selectedItems unmodifiable list of selected items
 * @param totalProfit   total non-negative profit of the selected items
 * @param totalWeight   total non-negative weight of the selected items
 */
public record Solution(List<Item> selectedItems, long totalProfit, long totalWeight) {

    public Solution {
        Objects.requireNonNull(selectedItems, "selectedItems must not be null");
        selectedItems = List.copyOf(selectedItems);
        if (totalProfit < 0) {
            throw new IllegalArgumentException("totalProfit must be non-negative, got: " + totalProfit);
        }
        if (totalWeight < 0) {
            throw new IllegalArgumentException("totalWeight must be non-negative, got: " + totalWeight);
        }
    }

    /**
     * Constructs a {@code Solution} from a collection of items, computing overflow-safe totals.
     *
     * @param items selected items
     * @return immutable {@code Solution}
     */
    public static Solution of(Collection<Item> items) {
        Objects.requireNonNull(items, "items must not be null");
        List<Item> copy = List.copyOf(items);
        long profit = 0L;
        long weight = 0L;
        for (Item item : copy) {
            profit = Math.addExact(profit, item.profit());
            weight = Math.addExact(weight, item.weight());
        }
        return new Solution(copy, profit, weight);
    }

    /**
     * Checks whether this solution is valid and feasible for the given {@link KP01} problem:
     * all selected items belong to the problem without duplicates, totals match exact item sums,
     * and total weight does not exceed the knapsack capacity.
     *
     * @param problem the knapsack problem instance
     * @return {@code true} if valid and feasible, {@code false} otherwise
     */
    public boolean isValid(KP01 problem) {
        if (problem == null) {
            return false;
        }
        Set<Item> problemItems = problem.getItems();
        Set<Item> seen = new HashSet<>(Math.max(selectedItems.size() * 2, 16));
        long computedProfit = 0L;
        long computedWeight = 0L;
        for (Item item : selectedItems) {
            if (!problemItems.contains(item) || !seen.add(item)) {
                return false;
            }
            if (item.profit() > Long.MAX_VALUE - computedProfit) {
                return false;
            }
            computedProfit += item.profit();
            if (item.weight() > problem.getCapacity() - computedWeight) {
                return false;
            }
            computedWeight += item.weight();
        }
        return computedProfit == totalProfit && computedWeight == totalWeight;
    }

    /**
     * Prints this solution to {@code System.out}.
     */
    public void print() {
        System.out.println(this);
    }

    @Override
    public String toString() {
        return String.format(
                Locale.ROOT,
                "Solution{items=%d, totalProfit=%d, totalWeight=%d, selectedIds=%s}",
                selectedItems.size(),
                totalProfit,
                totalWeight,
                selectedItems.stream().map(Item::id).toList()
        );
    }
}
