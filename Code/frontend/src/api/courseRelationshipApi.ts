import { api } from "./axios";

export type RelationType =
  | "PREREQUISITE"
  | "COREQUISITE"
  | "RECOMMENDED"
  | "EQUIVALENT";

export interface CourseRelationship {
  id: number;
  courseId: number;
  courseCode: string;
  courseName: string;
  relatedCourseId: number;
  relatedCourseCode: string;
  relatedCourseName: string;
  relationType: RelationType;
}

export interface CreateCourseRelationshipRequest {
  courseId: number;
  relatedCourseId: number;
  relationType: RelationType;
}

export const courseRelationshipApi = {
  getAll: async (): Promise<CourseRelationship[]> => {
    const res = await api.get<CourseRelationship[]>("/api/course-relationships");
    return res.data;
  },

  getByCourse: async (courseId: number): Promise<CourseRelationship[]> => {
    const res = await api.get<CourseRelationship[]>(
      `/api/course-relationships/course/${courseId}`
    );
    return res.data;
  },

  create: async (
    data: CreateCourseRelationshipRequest
  ): Promise<CourseRelationship> => {
    const res = await api.post<CourseRelationship>(
      "/api/course-relationships",
      data
    );
    return res.data;
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/api/course-relationships/${id}`);
  },
};