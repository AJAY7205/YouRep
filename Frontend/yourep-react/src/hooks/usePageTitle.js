import { useEffect } from 'react';

const usePageTitle = (title) => {
  useEffect(() => {
    document.title = title ? `${title} | YouRep` : 'YouRep';
  }, [title]);
};

export default usePageTitle;
