import re, sys
t = sys.argv[1]
x = open(sys.argv[2], encoding="utf-8", errors="ignore").read()
for m in re.finditer(r'<node [^>]*>', x):
    n = m.group(0)
    if re.search(r'text="' + re.escape(t) + '"', n):
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
        if b:
            a = list(map(int, b.groups()))
            print((a[0] + a[2]) // 2, (a[1] + a[3]) // 2)
            break
