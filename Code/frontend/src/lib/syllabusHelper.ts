export interface SyllabusMetadata {
  major: string;
  specialization: string;
}

export const getSyllabusMetadata = (courseCode: string, courseName: string): SyllabusMetadata => {
  const code = (courseCode || "").toUpperCase().trim();
  const name = (courseName || "").toLowerCase();

  // 1. General Education/Basic Sciences/Other departments
  if (
    code.startsWith("MA") ||
    code.startsWith("PH") ||
    code.startsWith("PE") ||
    code.startsWith("EN") ||
    code.startsWith("CH")
  ) {
    return {
      major: "Môn đại cương / Khác",
      specialization: "Đại cương / Không",
    };
  }

  // 2. Computer Engineering (CE) & Electrical Engineering (EE)
  if (
    code.startsWith("EE") ||
    name.includes("embedded") ||
    name.includes("vlsi") ||
    name.includes("digital logic") ||
    name.includes("micro-processing") ||
    name.includes("microprocessor") ||
    name.includes("digital system") ||
    name.includes("logic số") ||
    name.includes("vi xử lý") ||
    name.includes("hệ thống số") ||
    name.includes("hệ thống nhúng") ||
    name.includes("linh kiện điện tử")
  ) {
    return {
      major: "Kỹ thuật Máy tính",
      specialization: "Hệ thống Nhúng & IoT",
    };
  }

  // 3. Data Science (DS)
  if (
    name.includes("data science") ||
    name.includes("data visualization") ||
    name.includes("big data") ||
    name.includes("data mining") ||
    name.includes("data analysis") ||
    name.includes("regression") ||
    name.includes("optimization") ||
    name.includes("statistical methods") ||
    name.includes("analytics") ||
    name.includes("khoa học dữ liệu") ||
    name.includes("trực quan hóa") ||
    name.includes("phân tích dữ liệu") ||
    name.includes("hồi quy") ||
    name.includes("tối ưu hóa") ||
    name.includes("khai thác dữ liệu") ||
    name.includes("dữ liệu lớn")
  ) {
    return {
      major: "Khoa học Dữ liệu",
      specialization: "Phân tích Dữ liệu lớn",
    };
  }

  // 4. Computer Science (CS) - AI/ML/Theory
  if (
    name.includes("artificial intelligence") ||
    name.includes("deep learning") ||
    name.includes("machine learning") ||
    name.includes("trí tuệ nhân tạo") ||
    name.includes("học sâu") ||
    name.includes("học máy") ||
    name.includes("image processing") ||
    name.includes("xử lý ảnh") ||
    name.includes("discrete mathematics") ||
    name.includes("toán rời rạc") ||
    name.includes("theoretical") ||
    name.includes("mô hình lý thuyết") ||
    name.includes("graphics") ||
    name.includes("đồ họa")
  ) {
    return {
      major: "Khoa học Máy tính",
      specialization: "Trí tuệ Nhân tạo & Học máy",
    };
  }

  // 5. IT & Software Engineering (SE) - Networking & Security
  if (
    name.includes("network") ||
    name.includes("security") ||
    name.includes("administration") ||
    name.includes("internet of things") ||
    name.includes("iot") ||
    name.includes("mạng máy tính") ||
    name.includes("bảo mật") ||
    name.includes("quản trị hệ thống") ||
    name.includes("hướng mạng")
  ) {
    return {
      major: "Công nghệ Thông tin",
      specialization: "Mạng & Bảo mật",
    };
  }

  // Default fallback for IT prefix
  return {
    major: "Công nghệ Thông tin",
    specialization: "Phát triển Phần mềm & Lập trình Web",
  };
};

export const MAJORS = [
  "Khoa học Máy tính",
  "Công nghệ Thông tin",
  "Kỹ thuật Máy tính",
  "Khoa học Dữ liệu",
  "Môn đại cương / Khác",
];

export const SPECIALIZATIONS = [
  "Trí tuệ Nhân tạo & Học máy",
  "Phát triển Phần mềm & Lập trình Web",
  "Hệ thống Nhúng & IoT",
  "Mạng & Bảo mật",
  "Phân tích Dữ liệu lớn",
  "Đại cương / Không",
];
