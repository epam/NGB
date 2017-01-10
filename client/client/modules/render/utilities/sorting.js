const Math = window.Math;

export default class Sorting {
    static quickSort(items, ascending = true, selector = null, left = null, right = null) {
        if (selector === null || selector === undefined) {
            selector = item => item;
        }
        if (items.length > 1) {
            left = typeof left !== 'number' ? 0 : left;
            right = typeof right !== 'number' ? items.length - 1 : right;
            const index = Sorting.partition(items, ascending, left, right, selector);
            if (left < index - 1) {
                Sorting.quickSort(items, ascending, selector, left, index - 1);
            }
            if (index < right) {
                Sorting.quickSort(items, ascending, selector, index, right);
            }
        }
        return items;
    }

    static quickSortWithComparison(items, comparator, left = null, right = null) {
        if (!comparator)
            return items;
        if (items.length > 1) {
            left = typeof left !== 'number' ? 0 : left;
            right = typeof right !== 'number' ? items.length - 1 : right;
            const index = Sorting.partitionWithComparator(items, left, right, comparator);
            if (left < index - 1) {
                Sorting.quickSortWithComparison(items, comparator, left, index - 1);
            }
            if (index < right) {
                Sorting.quickSortWithComparison(items, comparator, index, right);
            }
        }
        return items;
    }

    static swap(items, firstIndex, secondIndex) {
        const temp = items[firstIndex];
        items[firstIndex] = items[secondIndex];
        items[secondIndex] = temp;
    }

    static partition(items, ascending, left, right, selector) {
        const pivot = selector(items[Math.floor((right + left) / 2)]);
        let i = left,
            j = right;
        while (i <= j) {
            if (!ascending) {
                while (i < right - 1 && selector(items[i]) > pivot) {
                    i++;
                }
                while (j > left && selector(items[j]) < pivot) {
                    j--;
                }
            }
            else {
                while (i < right - 1 && selector(items[i]) < pivot) {
                    i++;
                }
                while (j > left && selector(items[j]) > pivot) {
                    j--;
                }
            }
            if (i <= j) {
                Sorting.swap(items, i, j);
                i++;
                j--;
            }
        }
        return i;
    }

    static partitionWithComparator(items, left, right, comparator) {
        const pivot = items[Math.floor((right + left) / 2)];
        let i = left,
            j = right;
        while (i <= j) {
            while (i < right - 1 && comparator(items[i], pivot)) {
                i++;
            }
            while (j > left && comparator(pivot, items[j])) {
                j--;
            }
            if (i <= j) {
                Sorting.swap(items, i, j);
                i++;
                j--;
            }
        }
        return i;
    }
}
