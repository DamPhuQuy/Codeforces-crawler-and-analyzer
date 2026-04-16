-- ============================================================
-- V4__seed_sample_data.sql
-- Migration thứ 4: Seed dữ liệu mẫu (demo data).
--
-- Mục đích: Cho phép chạy demo app mà không cần crawl thực
-- Bao gồm:
--   - 3 users với thông tin thực tế (grandmaster, expert, newbie)
--   - 6 submissions (2 mỗi user)
--   - 4 analyses với kết quả phân tích AI giả lập
--
-- Dùng ON CONFLICT DO NOTHING để không bị lỗi nếu chạy lại.
-- ============================================================

-- ============================================================
-- SEED: Users (3 users với các level khác nhau)
-- ============================================================
INSERT INTO users (handle, display_name, rating, max_rating, rank, country, avatar_url, added_date) VALUES

    -- User 1: Grandmaster (dùng làm benchmark "không AI")
    (
        'tourist',
        'Gennady Korotkevich',
        3979,
        3979,
        'legendary grandmaster',
        'Belarus',
        'https://userpic.codeforces.org/422/title/50a270ed4a722867.jpg',
        NOW() - INTERVAL '10 days'
    ),

    -- User 2: Expert (mức trung bình khá)
    (
        'Petr',
        'Petr Mitrichev',
        3500,
        3552,
        'legendary grandmaster',
        'Russia',
        'https://userpic.codeforces.org/1/title/d3f48948222bf5cc.jpg',
        NOW() - INTERVAL '7 days'
    ),

    -- User 3: Newbie (người mới, hay dùng AI)
    (
        'demo_student',
        'Demo Student',
        850,
        920,
        'pupil',
        'Vietnam',
        '',
        NOW() - INTERVAL '3 days'
    )

ON CONFLICT (handle) DO NOTHING;

-- ============================================================
-- SEED: Submissions (6 bài với nội dung thực tế)
-- ============================================================
INSERT INTO submissions
    (user_handle, submission_id, contest_id, problem_index, problem_name, language, verdict, time_ms, memory_kb, source_code, submitted_at)
VALUES

    -- === tourist: Bài 1 - Dijkstra thuần túy, code CP style ===
    (
        'tourist',
        100000001,
        1854,
        'E',
        'Collapsing Strings',
        'GNU G++17 7.3.0',
        'OK',
        78,
        12300,
        E'#include<bits/stdc++.h>\nusing namespace std;\ntypedef long long ll;\n\nint main(){\n    ios_base::sync_with_stdio(0);\n    cin.tie(0);\n    int n;\n    cin>>n;\n    vector<string> s(n);\n    for(auto&x:s) cin>>x;\n    ll ans=0;\n    for(int i=0;i<n;i++)\n        for(int j=i+1;j<n;j++){\n            int k=0;\n            while(k<(int)s[i].size()&&k<(int)s[j].size()&&s[i][k]==s[j][k]) k++;\n            ans+=s[i].size()+s[j].size()-2*k;\n        }\n    // n^2 but n<=1e5 no wait read again\n    cout<<2*ans+"\\n";\n}',
        NOW() - INTERVAL '5 days'
    ),

    -- === tourist: Bài 2 - Segment Tree, code ngắn gọn kiểu CP ===
    (
        'tourist',
        100000002,
        1849,
        'D',
        'Beasts',
        'GNU G++17 7.3.0',
        'OK',
        124,
        28400,
        E'#include<bits/stdc++.h>\nusing namespace std;\n#define pb push_back\n#define all(x) x.begin(),x.end()\ntypedef long long ll;\n\nconst int N=3e5+5;\nint a[N],b[N];\nint main(){\n    int n; cin>>n;\n    for(int i=1;i<=n;i++) cin>>a[i];\n    for(int i=1;i<=n;i++) cin>>b[i];\n    vector<pair<int,int>> v;\n    for(int i=1;i<=n;i++) v.pb({a[i],i});\n    sort(all(v));\n    // greedy: pick smallest a[i] first\n    ll ans=0,mn=1e18;\n    for(auto[x,i]:v){\n        mn=min(mn,(ll)b[i]);\n        ans=max(ans,(ll)x*mn);\n    }\n    cout<<ans<<"\\n";\n}',
        NOW() - INTERVAL '4 days'
    ),

    -- === Petr: Bài 1 - DP bình thường ===
    (
        'Petr',
        200000001,
        1854,
        'B',
        'Sequence Game',
        'Java 17',
        'OK',
        312,
        65536,
        E'import java.util.*;\nimport java.io.*;\n\npublic class Main{\n    static BufferedReader br=new BufferedReader(new InputStreamReader(System.in));\n    static StringTokenizer st;\n    static int ni() throws Exception{\n        if(st==null||!st.hasMoreTokens()) st=new StringTokenizer(br.readLine());\n        return Integer.parseInt(st.nextToken());\n    }\n    public static void main(String[] a) throws Exception{\n        int t=ni();\n        while(t-->0){\n            int n=ni();\n            int[] arr=new int[n];\n            for(int i=0;i<n;i++) arr[i]=ni();\n            // check if valid\n            boolean ok=true;\n            for(int i=1;i<n;i++){\n                if(arr[i]<arr[i-1]&&(i==1||arr[i]<arr[i-2])) ok=false;\n            }\n            System.out.println(ok?"YES":"NO");\n        }\n    }\n}',
        NOW() - INTERVAL '3 days'
    ),

    -- === Petr: Bài 2 - BFS ngắn gọn ===
    (
        'Petr',
        200000002,
        1849,
        'C',
        'Turtle and Piggy Game',
        'Java 17',
        'OK',
        218,
        51200,
        E'import java.util.*;\npublic class Main{\n    public static void main(String[] a){\n        Scanner sc=new Scanner(System.in);\n        int t=sc.nextInt();\n        while(t-->0){\n            long n=sc.nextLong();\n            // always odd wins\n            if(n%2==1) System.out.println("Alice");\n            else System.out.println("Bob");\n        }\n    }\n}',
        NOW() - INTERVAL '2 days'
    ),

    -- === demo_student: Bài 1 - Code CÓ DẤU HIỆU DÙNG AI (đặt tên đẹp, comment chi tiết) ===
    (
        'demo_student',
        300000001,
        1854,
        'A',
        'Median Moves',
        'Java 17',
        'OK',
        156,
        32768,
        E'import java.util.Scanner;\n\n/**\n * Solution for Codeforces Problem 1854A - Median Moves\n * Approach: Mathematical observation about median properties\n * Time Complexity: O(1) per test case\n * Space Complexity: O(1)\n */\npublic class Main {\n\n    public static void main(String[] args) {\n        Scanner scanner = new Scanner(System.in);\n        int testCases = scanner.nextInt();\n\n        while (testCases-- > 0) {\n            int firstValue = scanner.nextInt();\n            int secondValue = scanner.nextInt();\n            int thirdValue = scanner.nextInt();\n\n            int result = computeMinimumMoves(firstValue, secondValue, thirdValue);\n            System.out.println(result);\n        }\n    }\n\n    /**\n     * Computes the minimum number of moves to make all three values equal to their median.\n     * Key insight: The median value already equals the target, so we only need\n     * to move the minimum and maximum values to match the median.\n     *\n     * @param a First value\n     * @param b Second value  \n     * @param c Third value\n     * @return Minimum moves required\n     */\n    private static int computeMinimumMoves(int a, int b, int c) {\n        int minimumValue = Math.min(a, Math.min(b, c));\n        int maximumValue = Math.max(a, Math.max(b, c));\n        int medianValue = a + b + c - minimumValue - maximumValue;\n\n        return (medianValue - minimumValue) + (maximumValue - medianValue);\n    }\n}',
        NOW() - INTERVAL '1 day'
    ),

    -- === demo_student: Bài 2 - Code thủ công hơn, ít dấu hiệu AI ===
    (
        'demo_student',
        300000002,
        1849,
        'A',
        'Game with Cards',
        'Java 17',
        'OK',
        93,
        28672,
        E'import java.util.*;\npublic class Main{\n    public static void main(String[] a){\n        Scanner sc=new Scanner(System.in);\n        int t=sc.nextInt();\n        while(t-->0){\n            int n=sc.nextInt(),m=sc.nextInt();\n            int[] x=new int[n],y=new int[m];\n            for(int i=0;i<n;i++) x[i]=sc.nextInt();\n            for(int i=0;i<m;i++) y[i]=sc.nextInt();\n            int mx=0;\n            for(int v:x) mx=Math.max(mx,v);\n            boolean ok=false;\n            for(int v:y) if(v>mx){ ok=true; break; }\n            System.out.println(ok?"YES":"NO");\n        }\n    }\n}',
        NOW() - INTERVAL '12 hours'
    )

ON CONFLICT (submission_id) DO NOTHING;

-- ============================================================
-- SEED: Analyses (kết quả phân tích AI giả lập)
-- ============================================================
INSERT INTO analyses
    (submission_id, data_structures, algorithms, ai_detected, ai_confidence,
     ai_indicators, highlighted_lines, time_complexity, space_complexity,
     difficulty_score, explanation)
SELECT
    s.id,
    a.data_structures,
    a.algorithms,
    a.ai_detected,
    a.ai_confidence,
    a.ai_indicators,
    a.highlighted_lines,
    a.time_complexity,
    a.space_complexity,
    a.difficulty_score,
    a.explanation
FROM submissions s
JOIN (VALUES

    -- === tourist sub 1: String processing, không dùng AI ===
    (100000001,
     '["Array", "String"]',
     '["Brute Force", "String Matching"]',
     FALSE, 0.05,
     '{"too_clean":{"detected":false,"evidence":""},"textbook_comments":{"detected":false,"evidence":""},"perfect_naming":{"detected":false,"evidence":"Dùng tên ngắn như x, k, ans"},"ai_pattern":{"detected":false,"evidence":""},"too_perfect":{"detected":false,"evidence":"Có comment debug ''no wait read again'' rất tự nhiên"},"wrong_style":{"detected":false,"evidence":"Import *bits/stdc++* đặc trưng CP style"}}',
     '[]',
     'O(n^2)',
     'O(n)',
     4,
     'Code theo style Codeforces điển hình: dùng bits/stdc++, tên biến ngắn (x, k, ans), không có comment giải thích dài dòng. Comment "no wait read again" cho thấy quá trình thực sự giải quyết vấn đề. Không có dấu hiệu AI.'
    ),

    -- === tourist sub 2: Greedy, không dùng AI ===
    (100000002,
     '["Array", "Vector", "Pair"]',
     '["Greedy", "Sorting"]',
     FALSE, 0.08,
     '{"too_clean":{"detected":false,"evidence":""},"textbook_comments":{"detected":false,"evidence":""},"perfect_naming":{"detected":false,"evidence":"Dùng v.pb, all(x), auto[x,i] - macro CP style"},"ai_pattern":{"detected":false,"evidence":""},"too_perfect":{"detected":false,"evidence":""},"wrong_style":{"detected":false,"evidence":""}}',
     '[]',
     'O(n log n)',
     'O(n)',
     5,
     'Code dùng các macro điển hình trong CP: #define pb push_back, #define all(x). Structured binding auto[x,i] của C++17. Approach greedy đơn giản và hiệu quả. Không có dấu hiệu sử dụng AI.'
    ),

    -- === Petr sub 1: DP, không dùng AI ===
    (200000001,
     '["Array"]',
     '["Greedy", "Linear Scan"]',
     FALSE, 0.12,
     '{"too_clean":{"detected":false,"evidence":""},"textbook_comments":{"detected":false,"evidence":""},"perfect_naming":{"detected":false,"evidence":"Dùng arr, ok, n - tên ngắn"},"ai_pattern":{"detected":false,"evidence":""},"too_perfect":{"detected":false,"evidence":""},"wrong_style":{"detected":false,"evidence":"Dùng BufferedReader + StringTokenizer - cách đọc input nhanh điển hình"}}',
     '[]',
     'O(n)',
     'O(n)',
     3,
     'Code Java sử dụng BufferedReader/StringTokenizer - kỹ thuật đọc input nhanh cổ điển trong competitive programming với Java. Logic kiểm tra mảng giảm dần đơn giản. Cấu trúc rõ ràng nhưng không có dấu hiệu AI.'
    ),

    -- === demo_student sub 1: PHÁT HIỆN DÙNG AI (confidence cao) ===
    (300000001,
     '["Array"]',
     '["Mathematical", "Observation"]',
     TRUE, 0.87,
     '{"too_clean":{"detected":true,"evidence":"Không có debug print, không có code thừa, formatting hoàn hảo với đúng 4 space indent"},"textbook_comments":{"detected":true,"evidence":"Javadoc @param, @return; comment giải thích Time/Space Complexity - không ai thi CP làm vậy"},"perfect_naming":{"detected":true,"evidence":"testCases, firstValue, secondValue, thirdValue, computeMinimumMoves, minimumValue, maximumValue, medianValue - quá chuẩn"},"ai_pattern":{"detected":true,"evidence":"Tách helper method computeMinimumMoves() riêng biệt - không cần thiết với bài này; class Javadoc"},"too_perfect":{"detected":false,"evidence":""},"wrong_style":{"detected":true,"evidence":"import java.util.Scanner thay vì import java.util.*; class Javadoc /** ... */"}}',
     '[{"line":1,"reason":"import đơn lẻ thay vì import java.util.*","category":"wrong_style"},{"line":3,"reason":"Class Javadoc comment - tuyệt đối không ai làm trong CP","category":"textbook_comments"},{"line":8,"reason":"Tên biến testCases thay vì t - dấu hiệu đặt tên AI","category":"perfect_naming"},{"line":10,"reason":"Tên biến firstValue/secondValue thay vì a,b,c","category":"perfect_naming"},{"line":17,"reason":"Tách helper method không cần thiết cho bài dễ thế này","category":"ai_pattern"},{"line":22,"reason":"@param Javadoc comment - tuyệt đối không ai thi CP viết vậy","category":"textbook_comments"}]',
     'O(1)',
     'O(1)',
     2,
     '⚠️ PHÁT HIỆN DẤU HIỆU DÙNG AI RẤT RÕ RÀNG (confidence: 87%)\n\n5/6 tiêu chí dương tính:\n• Đặt tên biến cực kỳ dài và chuẩn: testCases, firstValue, computeMinimumMoves... Dân CP dùng t, a, b, c.\n• Javadoc comment đầy đủ với @param, @return, Time/Space Complexity - không ai làm vậy trong contest\n• Helper method computeMinimumMoves() cho bài chỉ cần 1 dòng code là thừa hoàn toàn\n• import đơn lẻ thay vì import java.util.* - dấu hiệu AI generate code cẩn thận\n• Không có bất kỳ dấu vết debug hay thử nghiệm nào\n\nResult: Code này gần như chắc chắn được tạo bởi AI.'
    )

) AS a(submission_id_cf, data_structures, algorithms, ai_detected, ai_confidence,
       ai_indicators, highlighted_lines, time_complexity, space_complexity,
       difficulty_score, explanation)
ON s.submission_id = a.submission_id_cf
ON CONFLICT (submission_id) DO NOTHING;
