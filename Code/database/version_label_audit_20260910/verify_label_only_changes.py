from pathlib import Path
import re, json

def parse(path):
 text=Path(path).read_text(encoding='utf-16')
 tables={}; columns={}
 for t,body in re.findall(r'CREATE TABLE `([^`]+)` \((.*?)\) ENGINE=',text,re.S):
  columns[t]=re.findall(r'^  `([^`]+)`',body,re.M)
 for t,body in re.findall(r'INSERT INTO `([^`]+)` VALUES (.*?);\r?\n',text,re.S):
  rows=tables.setdefault(t,{})
  quote=False; escape=False; depth=0; start=0
  for i,c in enumerate(body):
   if escape: escape=False; continue
   if c=='\\' and quote: escape=True; continue
   if c=="'": quote=not quote; continue
   if quote: continue
   if c=='(':
    if depth==0: start=i
    depth+=1
   elif c==')':
    depth-=1
    if depth==0:
     row=body[start+1:i]; key=row.split(',',1)[0]
     # Composite relationship keys use first two columns.
     if t in ('assessment_clo','topic_clo','syllabus_book','clo_plo_mapping'): key=','.join(row.split(',')[:2])
     rows[key]=row
 return tables,columns

before, columns = parse('tmp/version-syllabus-before.sql')
after, _ = parse('tmp/version-syllabus-after.sql')
before, after = before['syllabus'], after['syllabus']
assert before.keys() == after.keys()
changed = 0
for key, row in before.items():
    if row == after[key]:
        continue
    parts = row.split(',', 4)
    label = "'v" + parts[2] + ".0'"
    assert row.replace(',' + parts[3] + ',', ',' + label + ',', 1) == after[key], key
    changed += 1
assert changed == 55
print('PASS: 60 rows preserved; only 55 version labels changed; all other fields identical.')
