import { useQuery } from "@tanstack/react-query";
import { programApi } from "@/api/programApi";

export const useProgramDiff = (id: number, oldCohortId: number, newCohortId: number) => {
  return useQuery({
    queryKey: ["programDiff", id, oldCohortId, newCohortId],
    queryFn: () => programApi.getDiff(id, oldCohortId, newCohortId),
    enabled: !!id && !!oldCohortId && !!newCohortId,
  });
};
