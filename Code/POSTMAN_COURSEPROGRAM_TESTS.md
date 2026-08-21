# CourseProgram API - Postman Test Guide

**Base URL:** `http://localhost:8080/api/course-programs`

## 1. CREATE - Tạo mới CourseProgram

**Method:** POST  
**Endpoint:** `/api/course-programs`

**Request Body:**
```json
{
  "courseId": 1,
  "programId": 1,
  "cohortId": 1,
  "courseTypeId": 1,
  "semesterSuggest": 1,
  "yearSuggest": 2024,
  "required": true
}
```

**cURL Command:**
```bash
curl --location 'http://localhost:8080/api/course-programs' \
--header 'Content-Type: application/json' \
--data '{
  "courseId": 1,
  "programId": 1,
  "cohortId": 1,
  "courseTypeId": 1,
  "semesterSuggest": 1,
  "yearSuggest": 2024,
  "required": true
}'
```

**Expected Response (201/200):**
```json
{
  "id": 1,
  "courseId": 1,
  "courseCode": "CS101",
  "courseName": "Introduction to Programming",
  "programId": 1,
  "programName": "Computer Science",
  "cohortId": 1,
  "cohortName": "K2024",
  "courseTypeId": 1,
  "courseTypeName": "Core",
  "semesterSuggest": 1,
  "yearSuggest": 2024,
  "required": true
}
```

---

## 2. GET ALL - Lấy tất cả CourseProgram

**Method:** GET  
**Endpoint:** `/api/course-programs`

**cURL Command:**
```bash
curl --location 'http://localhost:8080/api/course-programs'
```

**Expected Response (200):**
```json
[
  {
    "id": 1,
    "courseId": 1,
    "courseCode": "CS101",
    "courseName": "Introduction to Programming",
    "programId": 1,
    "programName": "Computer Science",
    "cohortId": 1,
    "cohortName": "K2024",
    "courseTypeId": 1,
    "courseTypeName": "Core",
    "semesterSuggest": 1,
    "yearSuggest": 2024,
    "required": true
  }
]
```

---

## 3. GET BY ID - Lấy CourseProgram theo ID

**Method:** GET  
**Endpoint:** `/api/course-programs/{id}`

**cURL Command:**
```bash
curl --location 'http://localhost:8080/api/course-programs/1'
```

**Expected Response (200):**
```json
{
  "id": 1,
  "courseId": 1,
  "courseCode": "CS101",
  "courseName": "Introduction to Programming",
  "programId": 1,
  "programName": "Computer Science",
  "cohortId": 1,
  "cohortName": "K2024",
  "courseTypeId": 1,
  "courseTypeName": "Core",
  "semesterSuggest": 1,
  "yearSuggest": 2024,
  "required": true
}
```

---

## 4. GET BY PROGRAM - Lấy CourseProgram theo Program ID

**Method:** GET  
**Endpoint:** `/api/course-programs/program/{programId}`

**cURL Command:**
```bash
curl --location 'http://localhost:8080/api/course-programs/program/1'
```

**Expected Response (200):**
```json
[
  {
    "id": 1,
    "courseId": 1,
    "courseCode": "CS101",
    "courseName": "Introduction to Programming",
    "programId": 1,
    "programName": "Computer Science",
    "cohortId": 1,
    "cohortName": "K2024",
    "courseTypeId": 1,
    "courseTypeName": "Core",
    "semesterSuggest": 1,
    "yearSuggest": 2024,
    "required": true
  }
]
```

---

## 5. GET BY COHORT - Lấy CourseProgram theo Cohort ID

**Method:** GET  
**Endpoint:** `/api/course-programs/cohort/{cohortId}`

**cURL Command:**
```bash
curl --location 'http://localhost:8080/api/course-programs/cohort/1'
```

**Expected Response (200):**
```json
[
  {
    "id": 1,
    "courseId": 1,
    "courseCode": "CS101",
    "courseName": "Introduction to Programming",
    "programId": 1,
    "programName": "Computer Science",
    "cohortId": 1,
    "cohortName": "K2024",
    "courseTypeId": 1,
    "courseTypeName": "Core",
    "semesterSuggest": 1,
    "yearSuggest": 2024,
    "required": true
  }
]
```

---

## 6. GET CURRICULUM - Lấy CourseProgram theo Program và Cohort (Query Params)

**Method:** GET  
**Endpoint:** `/api/course-programs/curriculum`  
**Query Parameters:** `programId` và `cohortId`

**cURL Command:**
```bash
curl --location 'http://localhost:8080/api/course-programs/curriculum?programId=1&cohortId=1'
```

**Expected Response (200):**
```json
[
  {
    "id": 1,
    "courseId": 1,
    "courseCode": "CS101",
    "courseName": "Introduction to Programming",
    "programId": 1,
    "programName": "Computer Science",
    "cohortId": 1,
    "cohortName": "K2024",
    "courseTypeId": 1,
    "courseTypeName": "Core",
    "semesterSuggest": 1,
    "yearSuggest": 2024,
    "required": true
  }
]
```

---

## 7. DELETE - Xóa CourseProgram theo ID

**Method:** DELETE  
**Endpoint:** `/api/course-programs/{id}`

**cURL Command:**
```bash
curl --location --request DELETE 'http://localhost:8080/api/course-programs/1'
```

**Expected Response (200/204):**
```
(No content or success message)
```

---

## Postman Collection Import (JSON)

Sao chép toàn bộ JSON dưới đây vào Postman > Import > Raw Text:

```json
{
  "info": {
    "name": "CourseProgram API",
    "description": "API tests for CourseProgram endpoints",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "1. CREATE CourseProgram",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"courseId\": 1,\n  \"programId\": 1,\n  \"cohortId\": 1,\n  \"courseTypeId\": 1,\n  \"semesterSuggest\": 1,\n  \"yearSuggest\": 2024,\n  \"required\": true\n}"
        },
        "url": {
          "raw": "http://localhost:8080/api/course-programs",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "course-programs"]
        }
      }
    },
    {
      "name": "2. GET ALL CoursePrograms",
      "request": {
        "method": "GET",
        "url": {
          "raw": "http://localhost:8080/api/course-programs",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "course-programs"]
        }
      }
    },
    {
      "name": "3. GET CourseProgram by ID",
      "request": {
        "method": "GET",
        "url": {
          "raw": "http://localhost:8080/api/course-programs/1",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "course-programs", "1"]
        }
      }
    },
    {
      "name": "4. GET CoursePrograms by Program",
      "request": {
        "method": "GET",
        "url": {
          "raw": "http://localhost:8080/api/course-programs/program/1",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "course-programs", "program", "1"]
        }
      }
    },
    {
      "name": "5. GET CoursePrograms by Cohort",
      "request": {
        "method": "GET",
        "url": {
          "raw": "http://localhost:8080/api/course-programs/cohort/1",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "course-programs", "cohort", "1"]
        }
      }
    },
    {
      "name": "6. GET Curriculum (Program + Cohort)",
      "request": {
        "method": "GET",
        "url": {
          "raw": "http://localhost:8080/api/course-programs/curriculum?programId=1&cohortId=1",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "course-programs", "curriculum"],
          "query": [
            {
              "key": "programId",
              "value": "1"
            },
            {
              "key": "cohortId",
              "value": "1"
            }
          ]
        }
      }
    },
    {
      "name": "7. DELETE CourseProgram",
      "request": {
        "method": "DELETE",
        "url": {
          "raw": "http://localhost:8080/api/course-programs/1",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "course-programs", "1"]
        }
      }
    }
  ]
}
```

---

## Lưu ý

- **IDs cần tồn tại:** courseId, programId, cohortId, courseTypeId phải đã tồn tại trong database
- **Base URL:** Thay `http://localhost:8080` nếu backend chạy trên host khác
- **Port:** Mặc định Spring Boot dùng port 8080
- **Content-Type:** Tất cả POST/PUT requests cần `Content-Type: application/json`
