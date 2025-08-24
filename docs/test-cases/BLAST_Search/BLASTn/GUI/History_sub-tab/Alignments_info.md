# View job details: View sequence details - alignments 

Test verifies
 - that User has the ability to view details about all matches (alignments) of the search query to the certain sequence from the BLAST search results

**Prerequisites**
 - Sequence for search:
`GAAATTGTCCAAAGATAGTTACCTCTCATAGGACCCCTCACTGACAGCATCCCCTAGCCGCACGTGACTAGTTAACTTAATTGAAAGTAAACGTTTAAAATTCTGTTCTTGAGTCGCGCTTCCCCCGTTTCAAATGCTTCATGTGGCTAGTGGCGACTCCGTTGGACAGCACAAACACGGAACGCTCCCATCCTCGCAGTGAGTTCAGCTACCGTCCCAAAAGATA`
 - Go to NGB
 - Close **Blast** panel if it is opened

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to  **Views** menu on the main toolbar| <li> **Views** menu is displayed <li> **BLAST** panel is displayed the last in the list|
| 3 | Select **BLAST** panel | <li>**BLAST** panel is displayed first at the right side in the additional panels <li> **Search** sub-tab is opened by default  <li> **blastn** search selector chosen by default |
| 4 | Enter sequence from **Prerequisites** in the **Query sequence** field | **Query sequence** field is filled by sequence from **Prerequisites**|
| 5 | Fill **Task title** field by any value (e.g. "View sequence details - alignments") | |
| 6 | Select **Homo_sapiens.GRCh38** database from the dropdown in the **Database** field||
| 7 | Select **blastn** value in the **Algorithm** dropdown field| |
| 8 | Click **Search** button and wait until status changed to **Done**| <li> **History** sub-tab is opened automatically <li> A new search task created with auto-generated ID |
| 9 | Click on last search ID in the **Task ID** column |The corresponding search results are displayed in the **Sequence table** of **History** sub-tab below **Blast parameters** section|
| 10 | Click on any **Sequence ID** (e.g. "13") in the Sequence table| <li> The form with details about all matches (alignments) of the search query to the certain sequence is opened in the same **History** sub-tab and contains following values: <li> **"<"** button - return to the sequences table view <li> **Sequence ID** and its **Definition** (displays to the right of "<" button) <li> Details about all matches of the search query to the current sequence (match blocks)|
| 11 | Look at **Sequence ID** |  <li> **Sequence ID** value **is a hyperlink** if the search was performed on the **NCBI** database - (goes to corresponding sequence page on NCBI by clicking on it) <li> **Sequence ID** value **is not a hyperlink** if the search was performed on the **"custom"** database (e.g. Homo_sapiens.GRCh38)| 
| 12 | Look at details of the search query matches | Each match block contains: <li> **Range (#) (positions)** of the current sequence where the match is defined (E.g. Range 1: 33065698 to 33065739) <li> **"View at track"** link - hyperlink to view the certain match (alignment) to the current sequence (**Visible** for **"custom"** databases **only**) <li> **Score**: ('bitscore') bits ('score') (Bitscore and score values can be taken from downloded file with results) (E.g. Score: 48.2 bits (52))  <li> **Expect**: (E-value) (E-value can be taken from downloded file with results) (E.g. Expect:0.012) <li> **Identities** - count of identities between sequences (by symbols) and its percent value (E.g. Identities:37/43 (86%)) <li> **Gaps** - count of gaps (by symbols) and its percent value (E.g. Gaps:1/43 (2%)) <li> **Strand**  of each sequence (query and subject) - plus or minus **(only for nucleotide sequences)** <li> Visual block with the conventional figure of the query string alignment to the current sequence segment|
| 13 | Look at visual block | Visual block contans: <li> **Query** and  **Subject seq** aligned segments <li> Every sequence has start and end position <li> Symbols that "link" the corresponding letters in both sequences has: <li> 1) **straight line** if letters are **equal** <li> 2) **nothing (empty)** if letters are **not equal** (mismatch) <li> 3) **minus symbol** ("-") in any sequence - for **gaps**  <br> Example: ![image](https://user-images.githubusercontent.com/45459424/119484997-40b3cc80-bd5f-11eb-81d0-b57999a9272c.png)|
| 14 | Reduce the width of the Blast panel | Both sequences is splitted on parts (up to panel width) and placed one above the other if aligned part of the query sequence/subject sequence is longer than width of the shown BLAST panel |