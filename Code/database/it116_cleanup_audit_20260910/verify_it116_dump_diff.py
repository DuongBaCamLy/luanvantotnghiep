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
before,columns=parse('tmp/it116-affected-tables-before-20260910.sql')
after,_=parse('tmp/it116-affected-tables-after-20260910.sql')
report=[]
for t in sorted(set(before)|set(after)):
 b=before.get(t,{}); a=after.get(t,{})
 deleted=sorted(b.keys()-a.keys()); added=sorted(a.keys()-b.keys()); changed=[k for k in b.keys()&a.keys() if b[k]!=a[k]]
 report.append(dict(table=t,deleted_count=len(deleted),deleted_keys=deleted,added=added,updated=changed))
 assert not added,(t,'unexpected added rows')
 if changed:
  assert t in ('syllabus','course_program','class_section'),t
  for k in changed:
   if t=='syllabus': assert k=='3013' and b[k].replace("'Version 1'","'v1.0'",1)==a[k]
   else:
    assert k==('196' if t=='course_program' else '11')
    idx=columns[t].index('syllabus_id'); values=b[k].split(','); assert values[idx]=='3050'; values[idx]='NULL'; assert ','.join(values)==a[k]
Path('tmp/it116-row-diff.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
for r in report: print(r['table'], 'deleted=',r['deleted_count'],'updated=',r['updated'])
print('PASS: no added rows; only 3013 version_label and 196/11 syllabus_id changed; all other retained rows identical.')
