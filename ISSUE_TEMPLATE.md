### OpenRefine GitHub Issue Policy

When opening a GitHub issue for OpenRefine, please ensure the following:

-   The issue is a **bug**, **performance issue**, **feature request**, **build issue**, or **documentation problem**. For small doc fixes, submit a Pull Request instead.
-   Complete the **Issue Template** in full.
-   Make sure the issue relates to the appropriate repository.
-   For individual support, please use **Stack Overflow** or community channels to keep the team focused on improving the tool for everyone.

---

### Issue Template with Examples

**Issue Type**: Bug, feature request, documentation, or build issue.
Select one: Bug / Performance Issue / Feature Request / Build Issue / Documentation

**Description**: A brief but clear explanation of the issue.
_Example_: "Filtering by date is not working as expected when applying a facet to large datasets."

**Steps to Reproduce**: How the issue can be replicated.

1. Open a dataset with over 10,000 rows.
2. Add a date facet.
3. Filter by a specific date range.
4. Observe that no results are returned.

**Expected Behavior**: What should happen.
_Example_: "The facet should return results within the specified date range."

**Actual Behavior**: What actually occurs.
_Example_: "No results are returned, even though data exists within the range."

**Environment Details**: Version info (OpenRefine, Java, OS).

-   **OpenRefine version**: e.g., 3.5.1
-   **Java version**: e.g., JDK 11
-   **OS**: e.g., Windows 10 / macOS 11

**Additional Information**: Any other relevant details.
_Example_: "The issue only occurs when the dataset exceeds 10,000 rows."
