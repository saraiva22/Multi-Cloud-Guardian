interface Sort {
  sorted: boolean;
  unsorted: boolean;
}

interface Pageable {
  sort: Sort;
  pageNumber: number;
  pageSize: number;
  offset: number;
}

export type PageResult<T> = {
  content: Array<T>;
  pageable: Pageable;
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
  size: number;
  number: number;
};
