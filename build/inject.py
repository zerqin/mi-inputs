import sys, zipfile, shutil

src, dex, dst = sys.argv[1], sys.argv[2], sys.argv[3]
shutil.copy(src, dst)
with zipfile.ZipFile(dst, "a", zipfile.ZIP_DEFLATED) as z:
    z.write(dex, "classes.dex")
print("injected", dex)
