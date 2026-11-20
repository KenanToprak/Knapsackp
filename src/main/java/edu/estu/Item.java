package edu.estu;

/**
 * Immutable representation of an item in a 0-1 Knapsack problem instance.
 *
 * @param id     unique or identifying integer label of the item
 * @param profit non-negative profit of selecting this item
 * @param weight non-negative weight of selecting this item
 */
public record Item(long id, long profit, long weight) {
    public Item {
        if (profit < 0) {
            throw new IllegalArgumentException("Item profit must be non-negative, got: " + profit);
        }
        if (weight < 0) {
            throw new IllegalArgumentException("Item weight must be non-negative, got: " + weight);
        }
    }
}
