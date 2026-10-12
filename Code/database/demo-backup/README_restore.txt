Database: curriculum_iu
Backup: curriculum_iu_20261013.sql.gz
SHA256: 4390C5E7D6639A74E2009183F7BC2D437C6CBB918E3D2F78514BF131EAB6CF14

Linux restore file:
cat curriculum_iu_20261013.sql.gz.part* > curriculum_iu_latest.sql.gz
gzip -t curriculum_iu_latest.sql.gz
sha256sum curriculum_iu_latest.sql.gz
