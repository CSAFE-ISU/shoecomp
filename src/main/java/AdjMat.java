import java.util.TreeSet;

public class AdjMat {
  int matsize;
  char[][] mat;

  AdjMat(int n) {
    this.matsize = n;
    this.mat = new char[n][n];
    for (int i = 0; i < n; ++i) {
      for (int j = 0; j < n; ++j) {
        mat[i][j] = 0;
      }
    }
  }

  void add_edge_qk(int qlen, int klen, int i1, int i2, int j1, int j2) {
    mat[i1 * klen + i2][j1 * klen + j2] = 1;
  }

  void get_pruned_indices(int lower_bound) {
    int[] degrees = new int[matsize];
    char[][] tmat = new char[matsize][matsize];
    int n = 0;
    int prev_n = matsize;
    char[] valid_verts = new char[matsize];
    TreeSet<Integer> bad_verts = new TreeSet<>();

    for (int i = 0; i < matsize; ++i) {
      degrees[i] = 1;
      valid_verts[i] = 1;
      for (int j = i + 1; j < matsize; j++) {
        if (mat[i][j] == 1 || mat[j][i] == 1) {
          degrees[i] += 1;
          tmat[i][j] = 1;
          tmat[j][i] = 1;
        }
      }
    }

    for (int i = 0; i < matsize; i++) {
      if (degrees[i] < lower_bound) {
        valid_verts[i] = 0;
        bad_verts.add(i);
      } else {
        n += 1;
      }
    }

    while (n != prev_n) {
      for (int x : bad_verts) {
        degrees[x] = 0;
        for (int j = 0; j < matsize; j++) {
          if (tmat[j][x] != 0) {
            degrees[j] -= 1;
          }
          tmat[x][j] = 0;
          tmat[j][x] = 0;
        }
      }

      prev_n = n;
      n = 0;

      for (int i = 0; i < matsize; i++) {
        if (degrees[i] < lower_bound) {
          valid_verts[i] = 0;
          if (bad_verts.contains(i)) {
            bad_verts.remove(i);
          } else {
            bad_verts.add(i);
          }
        } else {
          n += 1;
        }
      }
    }
  }
}
