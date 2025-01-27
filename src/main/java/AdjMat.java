import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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

  private static void fill_degrees(int[] deg, char[][] m, int N) {
    for (int i = 0; i < N; ++i) {
      deg[i] = 0;
    }
    for (int i = 0; i < N; ++i) {
      for (int j = 0; j < N; j++) {
        if (m[i][j] == 1) {
          deg[i] += 1;
        }
      }
    }
  }

  void add_edge_qk(int qlen, int klen, int i1, int i2, int j1, int j2) {
    mat[i1 * klen + i2][j1 * klen + j2] = 1;
  }

  AdjMat get_submat(ArrayList<Integer> verts) {
    int N = verts.size();
    AdjMat res = new AdjMat(N);
    for (int i = 0; i < N; i++) {
      int v1 = verts.get(i);
      for (int j = 0; j < N; j++) {
        int v2 = verts.get(j);
        if (this.mat[v1][v2] == 1 || this.mat[v2][v1] == 1) {
          res.mat[i][j] = 1;
        }
      }
    }
    return res;
  }

  ArrayList<Integer> get_pruned_indices(int lower_bound) {
    int[] degrees = new int[matsize];
    char[][] tmat = new char[matsize][matsize];
    int n;
    int prev_n;
    char[] valid_verts = new char[matsize];
    HashSet<Integer> bad_verts = new HashSet<>();
    ArrayList<Integer> res = new ArrayList<>();

    for (int i = 0; i < matsize; ++i) {
      for(int j = 0; j < matsize; j++) {
        if(mat[i][j] == 1 || mat[j][i] == 1) {
          tmat[i][j] = 1;
        } else {
          tmat[i][j] = 0;
        }
      }
      tmat[i][i] = 1;
      valid_verts[i] = 1;
    }
    n = matsize;
    prev_n = -1;

    do {
      // System.out.printf("n=%d, prev=%d\n", n, prev_n);
      for (int x : bad_verts) {
        if (degrees[x] == 0) continue;
        degrees[x] = 0;
        tmat[x][x] = 0;
        for (int j = 0; j < matsize; j++) {
          tmat[x][j] = 0;
          tmat[j][x] = 0;
        }
      }

      prev_n = n;
      n = 0;
      AdjMat.fill_degrees(degrees, tmat, matsize);
      for (int i = 0; i < matsize; i++) {
        if (degrees[i] >= lower_bound) {
          n += 1;
        } else if (degrees[i] != 0){
          // System.out.printf("%d: %d\n", i, degrees[i]);
          valid_verts[i] = 0;
          bad_verts.add(i);
        }
      }
    } while(n != prev_n);

    for (int i = 0; i < matsize; i++) {
      if (valid_verts[i] == 1) {
        res.add(i);
      }
    }
    return res;
  }
}
