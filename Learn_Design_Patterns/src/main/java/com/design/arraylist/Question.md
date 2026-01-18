# LLD Interview Problem: Design ArrayList

## Problem Statement

Design and implement a **dynamic array data structure**, similar to Java’s `ArrayList`.

Your task is to build a generic class `MyArrayList<E>` that internally uses an array to store elements and grows dynamically as elements are added.

You **must not** use Java’s built-in `ArrayList`, `List`, or other collection implementations.

This is an **SDE-2 level Low Level Design problem**.

---

## Functional Requirements

Your `MyArrayList<E>` should support the following operations:

### Core APIs

1. **add(E element)**
    - Appends an element to the end of the list.
    - Resize the internal array if capacity is full.

2. **add(int index, E element)**
    - Inserts an element at the given index.
    - Shift existing elements to the right.
    - Throw `IndexOutOfBoundsException` for invalid index.

3. **get(int index)**
    - Returns the element at the specified index.
    - O(1) time complexity.

4. **set(int index, E element)**
    - Replaces the element at the given index.
    - Returns the old value.

5. **remove(int index)**
    - Removes the element at the given index.
    - Shift remaining elements to the left.

6. **size()**
    - Returns the number of elements stored.

---

## Internal Design Requirements

- Use a **raw array** (`Object[]`) as the backing storage.
- Maintain:
    - `size` → number of elements
    - `capacity` → size of backing array
- Initial capacity = **10**
- Allow **null values**
- Maintain insertion order

---

## Resizing Strategy

- When the array is full:
    - Increase capacity using a growth factor
        - Preferred: `newCapacity = oldCapacity + (oldCapacity >> 1)` (1.5x, Java-style)
        - OR `newCapacity = oldCapacity * 2` (acceptable with justification)
- Copy existing elements into the new array

---

## Constraints

- Do NOT use `ArrayList`, `Vector`, or `List`
- Time Complexity Expectations:
    - `get`, `set` → O(1)
    - `add(E)` → O(1) amortized
    - `add(index)`, `remove(index)` → O(n)
- Space complexity should be optimized

---

## Edge Cases to Handle

- Adding at index `0`
- Adding at index `size`
- Removing last element
- Removing from empty list
- Resizing multiple times
- Handling `null` elements

---

## Follow-up Questions (Interviewer Will Ask)

1. Why is `ArrayList` faster than `LinkedList` for random access?
2. What is **amortized O(1)** complexity?
3. Why doesn’t `ArrayList` automatically shrink?
4. How does Java decide the new capacity?
5. What happens to references during resize?
6. Difference between `ArrayList` and `Vector`?
7. Is `ArrayList` thread-safe? How would you make it thread-safe?
8. What is `ensureCapacity()`?
9. Why is removal expensive in `ArrayList`?
10. How would you implement `remove(Object o)`?

---

## Bonus (Optional – For Strong Candidates)

Implement additional APIs:

- `boolean remove(Object o)`
- `void clear()`
- `void ensureCapacity(int minCapacity)`
- `void trimToSize()`
- `Iterator<E> iterator()`

---

## What the Interviewer Is Evaluating

- Understanding of dynamic arrays
- Memory vs performance tradeoffs
- Index and boundary safety
- Amortized analysis
- Clean, readable design
- Ability to explain *why*, not just *how*

---

## Expected Outcome

A clean, extensible `MyArrayList<E>` implementation with:
- Correct resizing logic
- Proper index validation
- Clear explanation of time and space complexity